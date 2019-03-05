package test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import eva.core.annotation.EvaService;
import eva.core.base.ResourceProvider;

@Component
public class BeanProvider implements ApplicationContextAware, ResourceProvider {

	private static ApplicationContext CONETXT;
	
	@Override
	public Object getSource(Class<?> interfaceClass) {
		return CONETXT.getBean(interfaceClass);
	}

	@Override
	public Collection<Class<?>> getEvaInterfaceClasses() {
		List<Class<?>> res = Lists.newArrayList();
		Map<String, Object> map = CONETXT.getBeansWithAnnotation(EvaService.class);
		if (Objects.nonNull(map.values())) {
			map.values().stream().forEach(v -> {
				res.add(v.getClass().getAnnotation(EvaService.class).interfaceClass());
			});
		}
		return res;
	}

	@Override
	public void setApplicationContext(ApplicationContext arg0) throws BeansException {
		CONETXT = arg0;
	}

}
