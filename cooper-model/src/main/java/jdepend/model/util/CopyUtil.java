package jdepend.model.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdepend.framework.exception.JDependException;
import jdepend.model.Component;
import jdepend.model.JavaClass;
import jdepend.model.JavaPackage;

public final class CopyUtil {

	private List<Component> targets = new ArrayList<Component>();
	private Map<String, JavaClass> javaClasses = new HashMap<String, JavaClass>();

	public List<Component> copy(List<Component> components) {
		// 创建JavaClass
		for (Component component : components) {
			for (JavaClass javaClass : component.getClasses()) {
				if (javaClasses.get(javaClass.getId()) == null) {
					newJavaClass(javaClass);
				}
			}
		}
		JavaClassCollection jClasses = new JavaClassCollection(javaClasses.values());
		// 补充JavaClassRelationItem的Current和Depend
		JavaClassUtil.supplyJavaClassRelationItem(jClasses);
		// 将JavaClassDetail中的字符串信息填充为对象引用
		JavaClassUtil.supplyJavaClassDetail(jClasses);

		// 创建JavaPackage
		for (Component component : components) {
			for (JavaPackage javaPackage : component.getJavaPackages()) {
				try {
					javaPackage.clone(javaClasses);
				} catch (JDependException e) {
					e.printStackTrace();
				}
			}
		}

		// 创建Component
		for (Component component : components) {
			try {
				targets.add(component.clone(javaClasses));
			} catch (JDependException e) {
				e.printStackTrace();
			}
		}
		return targets;

	}

	private JavaClass newJavaClass(JavaClass javaClass) {
		JavaClass newJavaClass = javaClasses.get(javaClass.getId());

		if (newJavaClass == null) {
			newJavaClass = javaClass.clone();
			javaClasses.put(newJavaClass.getId(), newJavaClass);
			// 添加内部类
			for (JavaClass innerClass : newJavaClass.getInnerClasses()) {
				javaClasses.put(innerClass.getId(), innerClass);
			}
		}
		return newJavaClass;
	}
}
