package b100.stackablefood.asm;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import b100.asmloader.ClassTransformer;
import b100.stackablefood.StackableFoodMod;
import b100.stackablefood.asm.utils.ASMHelper;
import b100.stackablefood.asm.utils.FindInstruction;
import b100.stackablefood.asm.utils.InjectHelper;

public class Transformers {
	
	class BlockTransformer extends ClassTransformer {

		@Override
		public boolean accepts(String className) {
			return className.equals("net/minecraft/core/block/Block");
		}

		@Override
		public void transform(String className, ClassNode classNode) {
			for(MethodNode method : classNode.methods) {
				if(method.name.equals("<init>")) {
					method.access = Opcodes.ACC_PUBLIC;
				}
			}
		}
		
	}
	
	private static final String listenerClass = "b100/stackablefood/asm/StackableFoodModListener";
	private static final String callbackInfoClass = "b100/stackablefood/asm/utils/CallbackInfo";
	
	// Initialize mod here instead of static Item init, because halplibe calls that early before Global.ACCESSOR exists
	class StatListTransformer extends ClassTransformer {

		@Override
		public boolean accepts(String className) {
			return className.equals("net/minecraft/core/achievement/stat/StatList");
		}

		@Override
		public void transform(String className, ClassNode classNode) {
			MethodNode method = ASMHelper.findMethod(classNode, "blocksAndItemsInitialized", null);
			List<AbstractInsnNode> returnNodes = ASMHelper.findAllInstructions(method.instructions, (n) -> n.getOpcode() == Opcodes.RETURN);
			if(returnNodes.size() == 0) {
				StackableFoodMod.print("No return node in blocksAndItemsInitialized!");
			}
			for(AbstractInsnNode node : returnNodes) {
				InsnList insert = new InsnList();
				insert.add(new FieldInsnNode(Opcodes.GETSTATIC, "net/minecraft/core/achievement/stat/StatList", "blocksInitialized", "Z"));
				insert.add(new FieldInsnNode(Opcodes.GETSTATIC, "net/minecraft/core/achievement/stat/StatList", "itemsInitialized", "Z"));
				insert.add(new MethodInsnNode(Opcodes.INVOKESTATIC, listenerClass, "onStatListInit", "(ZZ)V"));
				method.instructions.insertBefore(node, insert);
			}
		}
		
	}
	
	class EntityPlayerTransformer extends ClassTransformer {

		@Override
		public boolean accepts(String className) {
			return className.equals("net/minecraft/core/entity/player/EntityPlayer");
		}

		@Override
		public void transform(String className, ClassNode classNode) {
			classNode.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "remainingRegen", "I", null, null));
			classNode.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "regenCooldown", "I", null, null));
			
			transformTick(ASMHelper.findMethod(classNode, "tick", null));
		}
		
		private void transformTick(MethodNode method) {
			InsnList insert = new InsnList();
			
			insert.add(new VarInsnNode(Opcodes.ALOAD, 0));
			insert.add(new MethodInsnNode(Opcodes.INVOKESTATIC, listenerClass, "onTickPlayer", "(Lnet/minecraft/core/entity/player/EntityPlayer;)V"));
			
			method.instructions.insertBefore(method.instructions.getFirst(), insert);
		}
		
	}
	
	class ItemFoodTransformer extends ClassTransformer {

		@Override
		public boolean accepts(String className) {
			return className.equals("net/minecraft/core/item/ItemFood");
		}

		@Override
		public void transform(String className, ClassNode classNode) {
			classNode.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "slowHeal", "Z", null, null));
			
			ASMHelper.findField(classNode, "healAmount").access = Opcodes.ACC_PUBLIC;
			
			MethodNode method = ASMHelper.findMethod(classNode, "onItemRightClick", null);
			InsnList insert = new InjectHelper(listenerClass, callbackInfoClass).createMethodCallInject(classNode, method, "onItemFoodRightClick");
			method.instructions.insertBefore(method.instructions.getFirst(), insert);
		}
	}
	
	class HealthBarTransformer extends ClassTransformer {

		@Override
		public boolean accepts(String className) {
			return className.equals("net/minecraft/client/gui/hud/HealthBarComponent");
		}

		@Override
		public void transform(String className, ClassNode classNode) {
			transformRender(ASMHelper.findMethod(classNode, "render", null));
		}
		
		private void transformRender(MethodNode method) {
			List<AbstractInsnNode> nodes = ASMHelper.findAllInstructions(method.instructions, (n) -> FindInstruction.methodInsn(n, "drawTexturedModalRect"));
			if(nodes.size() != 8) {
				System.out.println("Unexpected number of 'drawTexturedModalRect' instructions: " + nodes.size()+", expected 8! Healthbar regeneration animation will not work!");
				return;
			}
			
			int[][] offsets = new int[][] {
				{0, 9},
				{1, 5},
				{2, 5},
				{3, 5},
				{4, 5},
				{5, 5},
				{6, 5},
				{7, 5},
			};
			
			List<AbstractInsnNode> nodesToModify = new ArrayList<>();
			
			for(int i=0; i < offsets.length; i++) {
				int nodeIndex = offsets[i][0];
				int offset = offsets[i][1];
				
				AbstractInsnNode node = nodes.get(nodeIndex);
				for(int j=0; j < offset; j++) {
					node = node.getPrevious();
				}
				
				if(!FindInstruction.varInsn(node, 14)) {
					System.out.println("Game overlay render code has changed! Healthbar regeneration animation will not work!");
					return;
				}
				
				nodesToModify.add(node);
			}
			
			for(int i=0; i < nodesToModify.size(); i++) {
				InsnList insert = new InsnList();
				insert.add(new VarInsnNode(Opcodes.ILOAD, 11)); // Heart index
				insert.add(new MethodInsnNode(Opcodes.INVOKESTATIC, listenerClass, "getHealthbarIconOffset", "(I)I"));
				insert.add(new InsnNode(Opcodes.ISUB));
				method.instructions.insert(nodesToModify.get(i), insert);
			}
		}
		
	}

}
