package b100.stackablefood.asm.utils;

public class CallbackInfo {
	
	private boolean cancelled;
	private Object returnValue;
	
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
	
	public boolean isCancelled() {
		return cancelled;
	}
	
	public void setReturnValue(Object returnValue) {
		this.returnValue = returnValue;
	}
	
	public Object getReturnValue() {
		return returnValue;
	}

}
