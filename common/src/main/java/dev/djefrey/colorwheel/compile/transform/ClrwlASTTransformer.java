package dev.djefrey.colorwheel.compile.transform;

import io.github.douira.glsl_transformer.ast.node.TranslationUnit;
import io.github.douira.glsl_transformer.ast.print.ASTPrinter;
import io.github.douira.glsl_transformer.ast.query.Root;
import io.github.douira.glsl_transformer.ast.query.RootSupplier;
import io.github.douira.glsl_transformer.ast.transform.ASTTransformer;
import io.github.douira.glsl_transformer.ast.transform.JobParameters;
import org.apache.commons.lang3.function.TriFunction;

import java.util.Map;
import java.util.function.Function;

public class ClrwlASTTransformer<J extends JobParameters> extends ASTTransformer<J, ClrwlTransformOutput>
{
    private Function<TranslationUnit, Map<Integer, String>> transformation;

    public ClrwlASTTransformer()
    {
        super();
    }

    public void setTransformation(TriFunction<TranslationUnit, Root, J, Map<Integer, String>> transformation)
    {
        this.transformation = unit -> transformation.apply(unit, unit.getRoot(), this.getJobParameters());
    }

    public ClrwlTransformOutput transform(String str, J parameters)
    {
        return this.transform(ClrwlTransformOutput.unprocessed(str), parameters);
    }

    @Override
    public ClrwlTransformOutput transform(RootSupplier rootSupplier, ClrwlTransformOutput input)
    {
        var translationUnit = parseTranslationUnit(rootSupplier, input.code());
        var outputs = transformation.apply(translationUnit);
        return new ClrwlTransformOutput(ASTPrinter.print(getPrintType(), translationUnit), outputs);
    }
}
