package com.mitchellbosecke.benchmark;


import com.mitchellbosecke.benchmark.model.Stock;
import io.quarkus.qute.Engine;
import io.quarkus.qute.ReflectionValueResolver;
import io.quarkus.qute.Template;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Benchmark for Qute templates.
 * <p>
 * https://quarkus.io/guides/qute-reference#standalone
 *
 * @author casid
 */
public class Qute extends BaseBenchmark {

    private Template template;
    private List<Stock> items;

    @Setup
    public void setup() throws Exception {
        items = Stock.dummyItems();

        Engine engine = Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver()).build();
        template = engine.parse(readTemplate());
    }

    @Benchmark
    public String benchmark() {
        return template.data("items", items).render();
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

    public static void main(String[] args) {
        Qute qute = new Qute();
        try {
            qute.setup();
            System.out.println(qute.benchmark());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
