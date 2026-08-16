package dev.djefrey.colorwheel.indirect;

public final class ClrwlBufferBindings
{
	public static final int TOTAL_BINDING_COUNT = 8;

	public static final int PAGE_FRAME_DESCRIPTOR = 0;
	public static final int INSTANCE = 1;
	public static final int DRAW_INSTANCE_INDEX = 2;
	public static final int MODEL = 3;
	public static final int DRAW = 4;

	public static final int LIGHT_LUT = 5;
	public static final int LIGHT_SECTION = 6;
	public static final int MATRICES = 7;

	private ClrwlBufferBindings()
	{
	}
}
