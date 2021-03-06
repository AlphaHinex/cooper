package jdepend.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jdepend.framework.context.JDependContext;
import jdepend.framework.context.Scope.SCOPE;
import jdepend.framework.log.LogUtil;
import jdepend.model.component.modelconf.Candidate;
import jdepend.model.component.modelconf.CandidateUtil;
import jdepend.model.relationtype.JavaClassRelationTypeMgr;
import jdepend.model.result.AnalysisResult;
import jdepend.model.util.JavaClassCollection;
import jdepend.model.util.ParseUtil;

import org.apache.bcel.Constants;

/**
 * The <code>JavaClass</code> class represents a Java class or interface.
 * 
 * @author <b>Abner</b>
 * 
 */

public final class JavaClass extends AbstractSubJDependUnit implements Candidate {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3619193239851101008L;

	private int access_flags;
	private String packageName;
	private Boolean abstractClassQualification;// 是否具有抽象类资格
	private Collection<String> imports;
	private int lineCount;
	private boolean isInner;
	private boolean stable = false;

	private String place;

	private JavaPackage javaPackage;
	private Component component;

	private int haveState = UnCalculate;// 缓存是否存在状态

	private JavaClassDetail detail;

	private Collection<JavaClassRelationItem> caItems = new ArrayList<JavaClassRelationItem>();

	private Collection<JavaClassRelationItem> ceItems = new ArrayList<JavaClassRelationItem>();

	private boolean isIncludeTransactionalAnnotation;

	private List<JavaClass> innerClasses = new ArrayList<JavaClass>();

	private JavaClass hostClass;

	private transient Collection<Method> methods;

	private transient Map<Method, Collection<Method>> overrideMethods;

	private transient Map<Method, Collection<Method>> subOverrideMethods;

	private transient Collection<JavaClass> supers;

	private transient Collection<JavaClass> superClasses;

	private transient Collection<JavaClass> interfaces;

	private transient Collection<JavaClass> subClasses;

	private transient Collection<JavaClass> subAllClasses;

	private transient Collection<JavaClassRelationItem> allCaItems;

	private transient Collection<JavaClassRelationItem> allCeItems;

	private transient Collection<JavaClass> allInnerClasses;

	private final static int UnCalculate = -2;
	private final static int HaveState = 1;
	private final static int NoHaveState = 0;
	private final static int Searched = -1;

	private final static String haveStateJavaClasses = "haveStateJavaClasses";

	private transient Collection<JavaClass> caList = null;
	private transient Collection<JavaClass> ceList = null;
	private transient Collection<JavaClass> relationList = null;
	private transient Collection<JavaClass> afferents = null;
	private transient Collection<JavaClass> efferents = null;

	public static final String Place = "JavaClass_Place";
	public static final String isPrivateElement = "JavaClass_isPrivateElement";
	public static final String Stable = "JavaClass_Stable";
	public static final String State = "JavaClass_State";

	public JavaClass(String name, boolean isInner, int access_flags) {
		this(name, isInner);
		this.access_flags = access_flags;
	}

	public JavaClass(String name, boolean isInner) {
		this.setName(name);
		this.packageName = JavaPackage.Default;
		this.imports = new HashSet<String>();
		this.detail = new JavaClassDetail(this);
		this.isInner = isInner;
		this.isIncludeTransactionalAnnotation = false;
		this.subClasses = new HashSet<JavaClass>();
	}

	public String getPlace() {
		return place;
	}

	public void setPlace(String place) {
		this.place = place;
	}

	@Override
	public String getId() {
		return CandidateUtil.getId(this);
	}

	/**
	 * 是否是内部类
	 * 
	 * @return
	 */
	public boolean isInnerClass() {
		if (this.getName().indexOf("$") > 0) {
			return true;
		} else {
			return false;
		}
	}

	public boolean containsInnerClass(JavaClass javaClass) {
		if (!javaClass.isInnerClass()) {
			return false;
		} else {
			return javaClass.getName().startsWith(this.getName());
		}
	}

	public boolean containedInnerClass(JavaClass javaClass) {
		if (!this.isInnerClass()) {
			return false;
		} else {
			return this.getName().startsWith(javaClass.getName());
		}
	}

	public void addInnerClass(JavaClass javaClass) {
		if (!this.innerClasses.contains(javaClass)) {
			this.innerClasses.add(javaClass);
		}
		javaClass.hostClass = this;
	}

	public JavaClass getHostClass() {
		if (this.hostClass.isInnerClass()) {
			return this.hostClass.getHostClass();
		} else {
			return hostClass;
		}
	}

	public Collection<JavaClass> getInnerClasses() {
		if (this.allInnerClasses == null) {
			this.allInnerClasses = new HashSet<JavaClass>();
			this.allInnerClasses.addAll(this.innerClasses);
			for (JavaClass innerClass : this.innerClasses) {
				this.allInnerClasses.addAll(innerClass.getInnerClasses());
			}
		}
		return this.allInnerClasses;
	}

	public void setPackageName(String name) {
		packageName = name;
	}

	public String getPackageName() {
		return packageName;
	}

	@Override
	public String getPath() {
		return packageName;
	}

	public Collection<String> getImportedPackages() {
		return imports;
	}

	/**
	 * 是否是分析目标中包含的Class
	 */
	public boolean isInner() {
		return this.isInner;
	}

	/**
	 * 是否为私有Class，与组件外部的Class没有关系
	 * 
	 * @return
	 */
	public boolean isPrivateElement() {
		return this.getAfferents().size() == 0 && this.getEfferents().size() == 0;
	}

	/**
	 * 是否被其他组件使用
	 * 
	 * @return
	 */
	public boolean isUsedByExternal() {
		return this.getAfferents().size() != 0;
	}

	@Override
	public AnalysisResult getResult() {
		return this.getComponent().getResult();
	}

	public JavaClassDetail getDetail() {
		return detail;
	}

	public void setDetail(JavaClassDetail detail) {
		this.detail = detail;
	}

	public Collection<Attribute> getAttributes() {
		return this.detail.getAttributes();
	}

	public Attribute getTheAttribute(String name) {
		return this.detail.getTheAttribute(name);
	}

	/**
	 * 得到类本身的方法列表（不包含继承父类的方法）
	 * 
	 * @return
	 */
	public Collection<Method> getSelfMethods() {
		return this.detail.getMethods();
	}

	public Method getTheMethod(String info) {
		for (Method method : this.getMethods()) {
			if (method.getInfo().equals(info)) {
				return method;
			}
		}
		return null;
	}

	/**
	 * 得到覆盖了父类方法的方法列表
	 * 
	 * @return
	 */
	public synchronized Map<Method, Collection<Method>> calOverrideMethods() {
		if (this.overrideMethods == null) {
			this.overrideMethods = new HashMap<Method, Collection<Method>>();
			for (JavaClass superClass : this.getSupers()) {
				for (Method method : superClass.getSelfMethods()) {
					if (!method.isConstruction() && (method.isPublic() || method.isProtected())) {
						for (Method selfMethod : this.getSelfMethods()) {
							if (selfMethod.isOverride(method)) {
								if (!this.overrideMethods.containsKey(selfMethod)) {
									this.overrideMethods.put(selfMethod, new HashSet<Method>());
								}
								this.overrideMethods.get(selfMethod).add(method);
							}
						}
					}
				}
			}
		}
		return this.overrideMethods;
	}

	/**
	 * 得到覆盖了父类方法的方法列表
	 * 
	 * @return
	 */
	public Collection<Method> getOverrideMethods() {
		return this.calOverrideMethods().keySet();
	}

	/**
	 * 得到指定方法覆盖的父类方法列表
	 * 
	 * @param method
	 * @return
	 */
	public Collection<Method> getOverridedMethods(Method method) {
		Collection<Method> overridedMethods = this.calOverrideMethods().get(method);
		if (overridedMethods == null) {
			return new HashSet<Method>();
		} else {
			return overridedMethods;
		}
	}

	/**
	 * 得到全部方法覆盖的父类方法列表
	 * 
	 * @param callee
	 * @return
	 */
	public Collection<Method> getOverridedMethods() {
		Collection<Method> overridedMethods = new HashSet<Method>();
		Map<Method, Collection<Method>> overrideMethods = this.calOverrideMethods();
		for (Method overrideMethod : overrideMethods.keySet()) {
			overridedMethods.addAll(overrideMethods.get(overrideMethod));
		}
		return overridedMethods;
	}

	/**
	 * 得到被子类覆盖的方法列表
	 * 
	 * @return
	 */
	public synchronized Map<Method, Collection<Method>> calSubOverrideMethods() {
		if (this.subOverrideMethods == null) {
			this.subOverrideMethods = new HashMap<Method, Collection<Method>>();
			for (JavaClass subClass : this.getSubClasses()) {
				for (Method method : subClass.getSelfMethods()) {
					if (!method.isConstruction() && (method.isPublic() || method.isProtected())) {
						for (Method selfMethod : this.getSelfMethods()) {
							if (method.isOverride(selfMethod)) {
								if (!this.subOverrideMethods.containsKey(selfMethod)) {
									this.subOverrideMethods.put(selfMethod, new HashSet<Method>());
								}
								this.subOverrideMethods.get(selfMethod).add(method);
							}
						}
					}
				}
			}
		}
		return this.subOverrideMethods;
	}

	/**
	 * 得到子类覆盖了的方法列表
	 * 
	 * @return
	 */
	public Collection<Method> getSubOverridedMethods() {
		return this.calSubOverrideMethods().keySet();
	}

	/**
	 * 得到指定方法被子类覆盖的方法列表
	 * 
	 * @param method
	 * @return
	 */
	public Collection<Method> getSubOverrideMethods(Method method) {
		Collection<Method> overridedMethods = this.calSubOverrideMethods().get(method);
		if (overridedMethods == null) {
			return new HashSet<Method>();
		} else {
			return overridedMethods;
		}
	}

	/**
	 * 得到覆盖的子类全部方法列表
	 * 
	 * @param callee
	 * @return
	 */
	public Collection<Method> getSubOverrideMethods() {
		Collection<Method> overridedMethods = new HashSet<Method>();
		Map<Method, Collection<Method>> overrideMethods = this.calSubOverrideMethods();
		for (Method overrideMethod : overrideMethods.keySet()) {
			overridedMethods.addAll(overrideMethods.get(overrideMethod));
		}
		return overridedMethods;
	}

	/**
	 * 得到类的构造函数列表
	 * 
	 * @return
	 */
	public Collection<Method> getConstructorMethods() {
		Collection<Method> constructors = new HashSet<Method>();
		for (Method method : this.getMethods()) {
			if (method.isConstruction()) {
				constructors.add(method);
			}
		}
		return constructors;
	}

	/**
	 * 得到其他类可以使用的方法列表（包含继承父类的方法）
	 * 
	 * @return
	 */
	public synchronized Collection<Method> getMethods() {
		if (this.methods == null) {
			boolean isOverride;
			this.methods = new HashSet<Method>();
			for (JavaClass superClass : this.getSupers()) {
				for (Method method : superClass.getSelfMethods()) {
					if (!method.isConstruction() && (method.isPublic() || method.isProtected())) {
						isOverride = false;
						L: for (Method selfMethod : this.getSelfMethods()) {
							if (selfMethod.isOverride(method)) {
								isOverride = true;
								break L;
							}
						}
						if (!isOverride) {
							this.methods.add(method);
						}
					}
				}
			}
			this.methods.addAll(this.getSelfMethods());
		}
		return this.methods;
	}

	public void addSelfMethod(Method method) {
		this.detail.addMethod(method);
	}

	public Collection<TableInfo> getTables() {
		return this.detail.getTables();
	}

	/**
	 * 得到所有的父类和接口列表
	 * 
	 * @return
	 */
	public synchronized Collection<JavaClass> getSupers() {
		if (this.supers == null) {
			supers = new HashSet<JavaClass>();
			supers.addAll(this.getSuperClasses());
			supers.addAll(this.getAllInterfaces());
		}
		return this.supers;
	}

	/**
	 * 得到所有的父类列表
	 * 
	 * @return
	 */
	private synchronized Collection<JavaClass> getSuperClasses() {
		if (this.superClasses == null) {
			this.superClasses = new HashSet<JavaClass>();
			JavaClass superClass = this.getSuperClass();
			if (superClass != null) {
				superClasses.add(superClass);
				superClasses.addAll(superClass.getSuperClasses());
			}
		}
		return this.superClasses;
	}

	/**
	 * 得到所有的接口列表
	 * 
	 * @return
	 */
	private synchronized Collection<JavaClass> getAllInterfaces() {
		if (this.interfaces == null) {
			this.interfaces = new HashSet<JavaClass>();
			for (JavaClass interfaceClass : this.getInterfaces()) {
				this.interfaces.add(interfaceClass);
				Collection<JavaClass> interfaceClasses = interfaceClass.getInterfaces();
				for (JavaClass interfaceClass1 : interfaceClasses) {
					this.interfaces.add(interfaceClass1);
					this.interfaces.addAll(interfaceClass1.getAllInterfaces());
				}
			}
		}
		return this.interfaces;
	}

	private Collection<JavaClass> getInterfaces() {
		return this.detail.getInterfaces();
	}

	private JavaClass getSuperClass() {
		return this.detail.getSuperClass();
	}

	public synchronized Collection<JavaClass> getSubClasses() {
		if (this.subAllClasses == null) {
			this.subAllClasses = new HashSet<JavaClass>();
			this.subAllClasses.addAll(this.subClasses);
			for (JavaClass subClass : subClasses) {
				this.subAllClasses.addAll(subClass.getSubClasses());
			}
		}
		return this.subAllClasses;
	}

	public synchronized void addSubClass(JavaClass javaClass) {
		this.subClasses.add(javaClass);
	}

	public Collection<JavaClassRelationItem> getCaItems() {
		if (this.allCaItems == null) {
			this.allCaItems = new HashSet<JavaClassRelationItem>(this.caItems);
			for (JavaClass innerClass : this.getInnerClasses()) {
				for (JavaClassRelationItem item : innerClass.getSelfCaItems()) {
					this.allCaItems.add(item.clone2());
				}
			}
		}
		return allCaItems;
	}

	public Collection<JavaClassRelationItem> getSelfCaItems() {
		return caItems;
	}

	public synchronized Collection<JavaClass> getCaList() {
		if (this.caList == null) {
			Collection<JavaClass> javaClasses = new HashSet<JavaClass>();
			for (JavaClassRelationItem item : getCaItems()) {
				javaClasses.add(item.getSource());
			}
			this.caList = javaClasses;
		}
		return this.caList;
	}

	public synchronized Collection<JavaClass> getRelationList() {
		if (this.relationList == null) {
			Collection<JavaClass> javaClasses = this.getCaList();
			for (JavaClass javaClass : this.getCeList()) {
				if (!javaClasses.contains(javaClass)) {
					javaClasses.add(javaClass);
				}
			}
			this.relationList = javaClasses;
		}
		return this.relationList;
	}

	public synchronized void addCaItems(JavaClassRelationItem caItem) {
		this.caItems.add(caItem);
	}

	public Collection<JavaClassRelationItem> getCeItems() {
		if (this.allCeItems == null) {
			this.allCeItems = new HashSet<JavaClassRelationItem>(this.ceItems);
			for (JavaClass innerClass : this.getInnerClasses()) {
				for (JavaClassRelationItem item : innerClass.getSelfCeItems()) {
					this.allCeItems.add(item.clone2());
				}
			}
		}
		return allCeItems;
	}

	public Collection<JavaClassRelationItem> getSelfCeItems() {
		return ceItems;
	}

	public synchronized Collection<JavaClass> getCeList() {
		if (this.ceList == null) {
			Collection<JavaClass> javaClasses = new HashSet<JavaClass>();
			for (JavaClassRelationItem item : getCeItems()) {
				javaClasses.add(item.getTarget());
			}
			this.ceList = javaClasses;
		}
		return this.ceList;
	}

	public synchronized void addCeItems(JavaClassRelationItem ceItem) {
		this.ceItems.add(ceItem);
	}

	public int getAbstractClassCount() {
		if (this.abstractClassQualification == null) {
			return this.isAbstract() ? 1 : 0;
		} else {
			return this.abstractClassQualification ? 1 : 0;
		}
	}

	public void setAbstractClassQualification(boolean abstractClassQualification) {
		this.abstractClassQualification = abstractClassQualification;
	}

	public int getConcreteClassCount() {
		if (getAbstractClassCount() == 1) {
			return 0;
		} else {
			return 1;
		}
	}

	public float getAbstractness() {
		return this.getAbstractClassCount();
	}

	public int getLineCount() {
		if (this.isInnerClass()) {
			return 0;
		} else {
			return this.lineCount;
		}
	}

	public void setLineCount(int lineCount) {
		this.lineCount = lineCount;
	}

	@Override
	public synchronized Collection<JavaClass> getAfferents() {
		if (this.afferents == null) {
			Collection<JavaClass> afferents = new HashSet<JavaClass>();

			for (JavaClass javaClass : this.getCaList()) {
				if (!this.component.containsClass(javaClass)) {
					afferents.add(javaClass);
				}
			}
			this.afferents = afferents;
		}
		return this.afferents;
	}

	@Override
	public synchronized Collection<JavaClass> getEfferents() {
		if (this.efferents == null) {
			Collection<JavaClass> efferents = new HashSet<JavaClass>();

			for (JavaClass javaClass : this.getCeList()) {
				if (!this.component.containsClass(javaClass)) {
					efferents.add(javaClass);
				}
			}
			this.efferents = efferents;
		}
		return this.efferents;
	}

	@Override
	public RelationDetail ceCouplingDetail(JDependUnit dependUnit) {

		RelationDetail detail = new RelationDetail();
		if (this.equals(dependUnit)) {
			return detail;
		}

		float intensity = 0;
		for (JavaClassRelationItem relationItem : this.getCeItems()) {
			if (dependUnit.containsClass(relationItem.getTarget())) {
				detail.addItem(relationItem);
				intensity += relationItem.getRelationIntensity();
			}
		}
		detail.setIntensity(intensity);

		return detail;
	}

	@Override
	public RelationDetail caCouplingDetail(JDependUnit dependUnit) {

		RelationDetail detail = new RelationDetail();
		if (this.equals(dependUnit)) {
			return detail;
		}

		float intensity = 0;
		for (JavaClassRelationItem relationItem : this.getCaItems()) {
			if (dependUnit.containsClass(relationItem.getSource())) {
				detail.addItem(relationItem);
				intensity += relationItem.getRelationIntensity();
			}
		}
		detail.setIntensity(intensity);

		return detail;
	}

	@Override
	public float caCoupling() {
		float intensity = 0;
		for (JavaClassRelationItem relationItem : this.getCaItems()) {
			if (!this.component.containsClass(relationItem.getSource())) {
				intensity += relationItem.getRelationIntensity();
			}
		}
		return intensity;
	}

	@Override
	public float ceCoupling() {
		float intensity = 0;
		for (JavaClassRelationItem relationItem : this.getCeItems()) {
			if (!this.component.containsClass(relationItem.getTarget())) {
				intensity += relationItem.getRelationIntensity();
			}
		}
		return intensity;
	}

	/**
	 * 计算在特定context环境下的内聚性
	 * 
	 * @param context
	 * @return
	 */
	@Override
	public float getCohesion() {
		return this.getGroupCohesionInfo().getCohesion();
	}

	@Override
	public float getBalance() {
		return this.getGroupInfoCalculator().getBalance();
	}

	@Override
	protected GroupInfoCalculator createGroupInfoCalculator() {
		return new GroupInfoCalculator(this);
	}

	@Override
	public int collectCycle(List<JDependUnit> list, Map<JDependUnit, Integer> knowledge) {

		if (!this.getComponent().getContainsCycle()) {
			return NoCycle;// 当类所在的组件不存在循环依赖时，类也不会存在循环依赖
		}

		if (list.size() > 20) {
			LogUtil.getInstance(JavaClass.class).systemWarning(
					"JavaClass[" + list.get(0).getName() + "] [" + this.getName() + "]collectCycle 搜索深度大于20停止搜索");
			return StopCheckCycle;// 搜索深度大于20时停止
		}

		if (list.contains(this)) {
			if (list.get(0).equals(this)) {
				for (JDependUnit unit : list) {
					if (!this.component.containsClass((JavaClass) unit)) {
						return Cycle;// 存在循环依赖
					}
				}
				knowledge.put(this, LocalCycle);
				return LocalCycle;// 存在局部循环依赖
			} else {
				// 通知其他组件存在循环依赖
				int index;
				L: for (index = 1; index < list.size(); index++) {
					if (list.get(index).equals(this)) {
						break L;
					}
				}
				List<JavaClass> otherCycles = new ArrayList<JavaClass>();
				Collection<Component> otherCycleComponents = new HashSet<Component>();
				for (int pos = index; pos < list.size(); pos++) {
					otherCycles.add((JavaClass) list.get(pos));
					otherCycleComponents.add(((JavaClass) list.get(pos)).getComponent());
				}
				if (otherCycleComponents.size() > 1) {
					for (JavaClass unit : otherCycles) {
						unit.setCycles(otherCycles);
					}
				}

				knowledge.put(this, LocalCycle);
				return LocalCycle;// 存在局部循环依赖
			}
		}

		list.add(this);// 将当前分析单元入栈

		if (this.getCeList().contains(list.get(0))) {// 直接依赖进行广度搜索
			for (JDependUnit unit : list) {
				if (!this.component.containsClass((JavaClass) unit)) {
					return Cycle;// 存在循环依赖
				}
			}
		}

		L: for (JavaClass efferent : this.getCeList()) {
			Integer rtnInteger = (Integer) knowledge.get(efferent);// 获取历史扫描数据
			if (rtnInteger == null) {// 没有扫描过的区域进行深度扫描
				int rtn = efferent.collectCycle(list, knowledge);// 深度搜索该区域
				if (rtn == Cycle) {// 存在循环依赖
					// 通知其他组件存在循环依赖
					for (int index = 1; index < list.size(); index++) {
						((JavaClass) list.get(index)).setCycles(list);
					}
					return Cycle;
				} else if (rtn == StopCheckCycle) {
					break L;// 搜索深度大于20时停止
				}
			}
		}

		list.remove(this);// 将当前分析单元出栈

		knowledge.put(this, NoCycle);// 记录该对象扫描过的结果

		return NoCycle;// 不存在循环依赖
	}

	public synchronized boolean isState() {
		if (this.haveState == UnCalculate) {
			this.haveState = this.searchState();
		}

		return this.haveState == HaveState;
	}

	public void setState(boolean haveState) {
		if (haveState) {
			this.haveState = HaveState;
		} else {
			this.haveState = NoHaveState;
		}
	}

	@Override
	public Collection<JavaPackage> getJavaPackages() {
		Collection<JavaPackage> javaPackages = new ArrayList<JavaPackage>();
		javaPackages.add(javaPackage);
		return javaPackages;
	}

	public JavaPackage getJavaPackage() {
		return javaPackage;
	}

	public void setJavaPackage(JavaPackage javaPackage) {
		this.javaPackage = javaPackage;
	}

	public Component getComponent() {
		return component;
	}

	public void setComponent(Component component) {
		this.component = component;
		for (JavaClass innerClass : this.getInnerClasses()) {
			innerClass.setComponent(component);
		}
	}

	public boolean containedComponent() {
		return this.component != null;
	}

	public void setStable(boolean b) {
		this.stable = b;
	}

	public boolean isStable() {
		return stable;
	}

	public boolean isIncludeTransactionalAnnotation() {
		return isIncludeTransactionalAnnotation;
	}

	public void setIncludeTransactionalAnnotation(boolean isIncludeTransactionalAnnotation) {
		this.isIncludeTransactionalAnnotation = isIncludeTransactionalAnnotation;
	}

	@Override
	public int getClassCount() {
		return 1;
	}

	@Override
	public Collection<JavaClass> getClasses() {
		Collection<JavaClass> javaClasses = new ArrayList<JavaClass>();
		javaClasses.add(this);
		return javaClasses;
	}

	@Override
	public boolean containsClass(JavaClass javaClass) {
		return this.equals(javaClass);
	}

	public JavaClass clone() {

		JavaClass obj = new JavaClass(this.getName(), this.isInner);

		obj.place = this.place;
		obj.access_flags = this.access_flags;
		obj.packageName = this.packageName;
		obj.lineCount = this.lineCount;
		obj.abstractClassQualification = this.abstractClassQualification;
		obj.stable = this.stable;
		obj.isIncludeTransactionalAnnotation = this.isIncludeTransactionalAnnotation;

		obj.setState(this.isState());
		obj.setType(this.getType());

		for (String importPackage : this.getImportedPackages()) {
			obj.addImportedPackage(importPackage);
		}

		obj.setDetail(this.getDetail().clone(obj));

		for (JavaClassRelationItem item : this.getSelfCaItems()) {
			JavaClassRelationItem newItem = item.clone();

			if (!obj.caItems.contains(newItem)) {
				obj.caItems.add(newItem);
			}
		}

		for (JavaClassRelationItem item : this.getSelfCeItems()) {
			JavaClassRelationItem newItem = item.clone();

			if (!obj.ceItems.contains(newItem)) {
				obj.ceItems.add(newItem);
			}
		}

		for (JavaClass innerClass : this.innerClasses) {
			obj.addInnerClass(innerClass.clone());
		}

		return obj;
	}

	public void supplyDetail(JavaClassCollection javaClasses) {
		this.getDetail().supply(javaClasses);
	}

	public void supplyJavaClassRelationItem(JavaClassCollection javaClasses) {

		Iterator<JavaClassRelationItem> it;
		JavaClassRelationItem relationItem;
		JavaClass dependClass;

		it = this.getSelfCaItems().iterator();
		while (it.hasNext()) {
			relationItem = it.next();
			dependClass = javaClasses.getTheClass(relationItem.getSourceJavaClassPlace(),
					relationItem.getSourceJavaClass());
			if (dependClass != null) {
				relationItem.setTarget(this);
				relationItem.setSource(dependClass);
				relationItem.setType(JavaClassRelationTypeMgr.getInstance().getType(relationItem.getTypeName()));
			} else {
				it.remove();
			}
		}
		it = this.getSelfCeItems().iterator();
		while (it.hasNext()) {
			relationItem = it.next();
			dependClass = javaClasses.getTheClass(relationItem.getTargetJavaClassPlace(),
					relationItem.getTargetJavaClass());
			if (dependClass != null) {
				relationItem.setTarget(dependClass);
				relationItem.setSource(this);
				relationItem.setType(JavaClassRelationTypeMgr.getInstance().getType(relationItem.getTypeName()));
			} else {
				it.remove();
			}
		}
	}

	/**
	 * 删除没有包含在javaClasses中的JavaClass引用
	 * 
	 * @param javaClasses
	 */
	public void filterExternalJavaClass(Collection<JavaClass> javaClasses) {
		Iterator<JavaClassRelationItem> it;
		for (JavaClass javaClass : javaClasses) {
			it = javaClass.getSelfCaItems().iterator();
			while (it.hasNext()) {
				if (!javaClasses.contains(it.next().getSource())) {
					it.remove();
				}
			}
			it = javaClass.getSelfCeItems().iterator();
			while (it.hasNext()) {
				if (!javaClasses.contains(it.next().getTarget())) {
					it.remove();
				}
			}
			// this.getDetail().filterExternalJavaClass(javaClasses);
		}
	}

	@Override
	public int compareTo(JDependUnit o) {
		return this.getId().compareTo(((JavaClass) o).getId());
	}

	@Override
	public int hashCode() {
		final int prime = 32;
		int result = 1;
		result = prime * result + ((this.getId() == null) ? 0 : this.getId().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;

		JavaClass other = (JavaClass) obj;
		if (!getId().equals(other.getId()))
			return false;

		return true;
	}

	@Override
	public String toString() {
		return this.detail.toString();
	}

	@Override
	public void clear() {
		super.clear();

		caList = null;
		ceList = null;
		relationList = null;
		afferents = null;
		efferents = null;

		supers = null;
		superClasses = null;
		interfaces = null;
		subAllClasses = null;

		allCeItems = null;
		allCaItems = null;
		allInnerClasses = null;
	}

	public final boolean isPublic() {
		return (access_flags & Constants.ACC_PUBLIC) != 0;
	}

	public final boolean isPrivate() {
		return (access_flags & Constants.ACC_PRIVATE) != 0;
	}

	public final boolean isProtected() {
		return (access_flags & Constants.ACC_PROTECTED) != 0;
	}

	public final boolean isStatic() {
		return (access_flags & Constants.ACC_STATIC) != 0;
	}

	public final boolean isFinal() {
		return (access_flags & Constants.ACC_FINAL) != 0;
	}

	public final boolean isInterface() {
		return (access_flags & Constants.ACC_INTERFACE) != 0;
	}

	public final boolean isAbstract() {
		return (access_flags & Constants.ACC_ABSTRACT) != 0;
	}

	public final boolean isAnnotation() {
		return (access_flags & Constants.ACC_ANNOTATION) != 0;
	}

	public final boolean isEnum() {
		return (access_flags & Constants.ACC_ENUM) != 0;
	}

	/**
	 * 在组件中是否孤立，与组件中的其他Class都没有关系
	 * 
	 * @return
	 */
	public boolean isAlone() {
		for (JavaClass relationClass : this.getRelationList()) {
			if (this.getComponent().containsClass(relationClass)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 是否存在可变基本类型
	 * 
	 * @param types
	 * @return
	 */
	private boolean exitChangeBaseType() {
		for (Attribute attribute : this.getAttributes()) {
			if (!attribute.isFinal() && !attribute.isStatic()) {
				for (String type : attribute.getTypes()) {
					if (ParseUtil.isBaseType(type) || ParseUtil.isStateClassType(type)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public void calImportedPackages() {
		String packageName;

		if (this.detail.getSuperClassName() != null) {
			packageName = ParseUtil.getPackageName(this.detail.getSuperClassName());
			addImportedPackage(packageName);
		}

		for (String name : this.detail.getInterfaceNames()) {
			packageName = ParseUtil.getPackageName(name);
			addImportedPackage(packageName);
		}

		for (String name : this.detail.getAttributeTypes()) {
			packageName = ParseUtil.getPackageName(name);
			addImportedPackage(packageName);
		}

		for (String name : this.detail.getParamTypes()) {
			packageName = ParseUtil.getPackageName(name);
			addImportedPackage(packageName);
		}

		for (String name : this.detail.getVariableTypes()) {
			packageName = ParseUtil.getPackageName(name);
			addImportedPackage(packageName);
		}
	}

	@Override
	public Object getValue(String metrics) {

		switch (metrics) {

		case JavaClass.Place:
			return this.getPlace();

		case JavaClass.State:
			if (this.isState()) {
				return MetricsMgr.HaveState;
			} else {
				return MetricsMgr.NoValue;
			}

		case JavaClass.Stable:
			if (this.isStable()) {
				return MetricsMgr.Stability;
			} else {
				return MetricsMgr.NoValue;
			}

		case JavaClass.isPrivateElement:
			if (!this.isUsedByExternal()) {
				return MetricsMgr.Private;
			} else {
				return MetricsMgr.NoValue;
			}

		case MetricsMgr.Ca:
			return this.getAfferentCoupling() + "|" + this.getCaList().size();

		case MetricsMgr.Ce:
			return this.getEfferentCoupling() + "|" + this.getCeList().size();

		default:
			return super.getValue(metrics);
		}

	}

	@Override
	public int size() {
		return this.getLineCount();
	}

	/**
	 * 搜索该JavaClass是否存在状态
	 * 
	 * @return 1 存在；0 不存在； -1 该javaClass已经扫描过了。
	 */
	private int searchState() {

		// 判断该javaClass是否扫描过
		Collection<JavaClass> javaClasses = (Collection<JavaClass>) JDependContext.getInfo(SCOPE.THREAD_SCOPSE,
				haveStateJavaClasses);
		if (javaClasses == null) {// 建立历史列表
			javaClasses = new HashSet<JavaClass>();
			JDependContext.setInfo(SCOPE.THREAD_SCOPSE, haveStateJavaClasses, javaClasses);
		} else if (javaClasses.contains(this)) {// 扫描过
			return Searched;
		}
		// 放入扫描列表
		javaClasses.add(this);

		// 判断是否存在可变基本类型
		if (exitChangeBaseType()) {
			// 清空扫描列表
			JDependContext.setInfo(SCOPE.THREAD_SCOPSE, haveStateJavaClasses, null);
			return HaveState;
		}

		int result;

		// 判断属性
		for (Attribute attribute : this.getAttributes()) {
			for (JavaClass javaClass : attribute.getTypeClasses()) {
				result = javaClass.searchState();
				if (result != Searched) {
					return result;
				}
			}
		}

		// 判断父类是否存在状态
		if (this.getSuperClass() != null) {
			result = this.getSuperClass().searchState();
			if (result != Searched) {
				return result;
			}
		}

		if (javaClasses.iterator().next().equals(this)) {// 搜索完毕时，清空搜索列表
			JDependContext.setInfo(SCOPE.THREAD_SCOPSE, haveStateJavaClasses, null);
		}
		return NoHaveState;
	}

	private boolean addImportedPackage(String jPackage) {
		if (jPackage != null && !jPackage.equals(packageName) && !imports.contains(jPackage)) {
			imports.add(jPackage);
			return true;
		} else {
			return false;
		}
	}

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		this.subClasses = new HashSet<JavaClass>();
	}

}
