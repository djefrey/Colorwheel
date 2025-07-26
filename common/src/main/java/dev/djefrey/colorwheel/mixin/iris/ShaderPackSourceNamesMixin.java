package dev.djefrey.colorwheel.mixin.iris;

import com.google.common.collect.ImmutableList;
import dev.djefrey.colorwheel.ClrwlProgramId;
import net.irisshaders.iris.shaderpack.include.ShaderPackSourceNames;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ShaderPackSourceNames.class)
public abstract class ShaderPackSourceNamesMixin
{
	@Invoker
	private static void callAddStarts(ImmutableList.Builder<String> potentialFileNames, String baseName) {}

	@Inject(method = "findPotentialStarts",
			at = @At(value = "INVOKE", target = "com/google/common/collect/ImmutableList$Builder.build ()Lcom/google/common/collect/ImmutableList;"),
			locals = LocalCapture.CAPTURE_FAILEXCEPTION,
			remap = false)
	private static void injectPotentialStats(CallbackInfoReturnable<ImmutableList<String>> cir, ImmutableList.Builder<String> builder)
	{
		for (var programId : ClrwlProgramId.values())
		{
			callAddStarts(builder, programId.programName());
		}
	}
}
