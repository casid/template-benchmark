package com.mitchellbosecke.benchmark;

import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.beetl.core.resource.ClasspathResourceLoader;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

import java.io.IOException;
import java.util.Map;

public class Beetl extends BaseBenchmark {

	private Map<String, Object> context;

	GroupTemplate gt = null;

	@Setup
	public void setup() throws IOException {
		ClasspathResourceLoader resourceLoader = new MyClasspathResourceLoader("/");
		Configuration cfg = Configuration.defaultConfiguration();
		cfg.setCacheOutPutBuffer(10240);
		cfg.setStatementStart("@");
		cfg.setStatementEnd(null);
		cfg.getResourceMap().put("autoCheck", "false");
		gt = new GroupTemplate(resourceLoader, cfg);

	}

	@Benchmark
	public String benchmark() throws IOException {
		Template template = gt.getTemplate("/templates/stocks.beetl.html");
		template.binding(getContext());
		return template.render();
	}

	static class MyClasspathResourceLoader extends ClasspathResourceLoader {

		public MyClasspathResourceLoader(String root) {
			super(root);
		}

		@Override
		public void init(GroupTemplate gt) {
			Map<String, String> resourceMap = gt.getConf().getResourceMap();

			if (this.charset == null) {
				this.charset = resourceMap.get("charset");

			}

			this.setAutoCheck(false);

		}
	}
}
