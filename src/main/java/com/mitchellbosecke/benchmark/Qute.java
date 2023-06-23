package com.mitchellbosecke.benchmark;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

import com.mitchellbosecke.benchmark.model.Stock;

import io.quarkus.gizmo.ClassOutput;
import io.quarkus.qute.Engine;
import io.quarkus.qute.EngineBuilder;
import io.quarkus.qute.EvalContext;
import io.quarkus.qute.Results;
import io.quarkus.qute.Template;
import io.quarkus.qute.ValueResolver;
import io.quarkus.qute.generator.ValueResolverGenerator;

/**
 * Benchmark for Qute templates.
 * <p>
 * https://quarkus.io/guides/qute-reference#standalone
 *
 * @author casid
 */
public class Qute extends BaseBenchmark {

    private Template template;
    private Map<String, Object> context;

    @Setup
    public void setup() throws Exception {
        context = getContext();

        IndexView index = index(Stock.class);
        ClassOutput classOutput = new ClassOutput() {

            @Override
            public void write(String name, byte[] data) {
                try {
                    File dir = new File("target/classes/", name.substring(0, name.lastIndexOf("/")));
                    dir.mkdirs();
                    File output = new File("target/classes/", name + ".class");
                    Files.write(output.toPath(), data);
                } catch (IOException e) {
                    throw new IllegalStateException("Cannot dump the class: " + name, e);
                }

            }
        };

        ValueResolverGenerator generator = ValueResolverGenerator.builder().setIndex(index).setClassOutput(classOutput)
                .addClass(index.getClassByName(DotName.createSimple(Stock.class.getName()))).build();
        generator.generate();

        // Build engine
        EngineBuilder builder = Engine.builder().addDefaults().useAsyncTimeout(false);
        // Add the generated value resolvers that would be normally provided by Quarkus extension
        for (String resolverClass : generator.getGeneratedTypes()) {
            builder.addValueResolver(createResolver(resolverClass.replace("/", ".")));
        }
        // This value resolver adds a computed property "isNegative" to the java.lang.Double
        // In Quarkus, this would be implemented as a template extension method
        builder.addValueResolver(
                new ValueResolver() {
                    @Override
                    public boolean appliesTo(EvalContext context) {
                        return (context.getBase().getClass() == Double.class) && context.getName().equals("isNegative");
                    }

                    @Override
                    public CompletionStage<Object> resolve(EvalContext context) {
                        return ((Double) context.getBase()) < 0 ? Results.TRUE : Results.FALSE;
                    }
                });

        Engine engine = builder.build();
        template = engine.parse(readTemplate());
    }

    @Benchmark
    public String benchmark() {
        return template.render(context);
    }

    private String readTemplate() throws Exception {
        String name = "templates/stocks.qute.html";
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(name)) {
            if (is == null) {
                throw new FileNotFoundException(name + " does not exist.");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    ValueResolver createResolver(String resolverClassName) {
        try {
            ClassLoader cl = Thread.currentThread()
                    .getContextClassLoader();
            if (cl == null) {
                cl = Qute.class.getClassLoader();
            }
            Class<?> resolverClazz = cl.loadClass(resolverClassName);
            if (ValueResolver.class.isAssignableFrom(resolverClazz)) {
                return (ValueResolver) resolverClazz.newInstance();
            }
            throw new IllegalStateException("Not a value resolver: " + resolverClassName);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new IllegalStateException("Unable to create resolver: " + resolverClassName, e);
        }
    }

    static Index index(Class<?>... classes) throws IOException {
        Indexer indexer = new Indexer();
        for (Class<?> clazz : classes) {
            try (InputStream stream = Qute.class.getClassLoader()
                    .getResourceAsStream(clazz.getName().replace('.', '/') + ".class")) {
                indexer.index(stream);
            }
        }
        return indexer.complete();
    }
}
