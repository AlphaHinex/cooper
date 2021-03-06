package jdepend.knowledge.pattern.impl1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import jdepend.knowledge.pattern.PatternInfo;
import jdepend.model.Attribute;
import jdepend.model.InvokeItem;
import jdepend.model.JavaClass;
import jdepend.model.Method;

public final class ObserverIdentifyer extends AbstractPatternIdentifyer {

	@Override
	public String getExplain() {
		StringBuilder explain = new StringBuilder();
		explain.append("&nbsp;&nbsp;&nbsp;&nbsp;<strong>观察者模式</strong><br>");
		explain.append("&nbsp;&nbsp;&nbsp;&nbsp;1、属性有接口；2、在方法中调用该接口的方法，并将自身作为参数；3、该接口有实现。<br><br>");
		return explain.toString();
	}

	@Override
	public Collection<PatternInfo> identify(Collection<JavaClass> javaClasses) {
		Collection<Method> observereMethods;
		Collection<PatternInfo> rtn = new ArrayList<PatternInfo>();
		for (JavaClass javaClass : javaClasses) {
			observereMethods = new HashSet<Method>();
			// 识别observers
			for (Attribute attribute : javaClass.getAttributes()) {
				for (JavaClass observer : attribute.getTypeClasses()) {
					if (observer.isInterface()) {// 1、属性有接口
						L: for (Method method : observer.getSelfMethods()) {
							for (String argType : method.getArgumentTypes()) {// 2、方法参数将调用者作为参数
								if (javaClass.getName().equals(argType)) {
									observereMethods.add(method);
									break L;
								}
							}
						}
					}

				}
			}
			// 判断是否在当前Class调用该方法
			M: for (Method method : javaClass.getSelfMethods()) {
				for (InvokeItem invokeItem : method.getInvokeItems()) {
					if (observereMethods.contains(invokeItem.getCallee())) {
						rtn.add(new PatternInfo(javaClass, javaClass.getName() + "." + method.getName()));
						break M;
					}
				}
			}
		}
		return rtn;
	}
}
