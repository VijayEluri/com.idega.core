/*
 * Created on 11.8.2003 by  tryggvil in project com.project
 */
package com.idega.repository.data;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.idega.idegaweb.IWMainApplication;
import com.idega.util.CoreConstants;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;

/**
 * A class to hold a registry over classes that have been moved between packages or renamed (refactored)
 * Copyright (C) idega software 2003
 * @author <a href="mailto:tryggvi@idega.is">Tryggvi Larusson</a>
 * @version 1.0
 */
public class RefactorClassRegistry implements Singleton {

	private static Instantiator instantiator = new Instantiator() {
		@Override
		public Object getInstance() {
			return new RefactorClassRegistry();
		}
	};

	//This constructor should not be called
	protected RefactorClassRegistry() {
	}

	private static String getDecryptedClassName(String className) {
		if (StringUtil.isEmpty(className)) {
			return className;
		}
		if (className.indexOf(CoreConstants.DOT) != -1) {
			return className;
		}

		String decryptedClassName = IWMainApplication.decryptClassName(className);
		return decryptedClassName;
	}

	public static <T> Class<T> forName(String className) throws ClassNotFoundException {
		return forName(className, true);
	}

	@SuppressWarnings("unchecked")
	private static <T> Class<T> forName(String className, boolean reTry) throws ClassNotFoundException {
		// first try to find the class
		try {
			className = getDecryptedClassName(className);

			try {
				return (Class<T>) Class.forName(className);
			} catch (ClassNotFoundException ex) {
				return RefactorClassRegistry.getInstance().findClass(className, ex);
			}
		} catch (Exception e) {
			String defaultClass = null;
			if (reTry) {
				defaultClass = IWMainApplication.getDefaultIWMainApplication().getSettings().getProperty("iw_class_registry.default_class");
			}
			if (StringUtil.isEmpty(defaultClass)) {
				throw new ClassNotFoundException("Class with name " + className + " can not be found", e);
			} else {
				Logger.getLogger(RefactorClassRegistry.class.getName()).info("Failed to load class by provided name: " + className + ", will try to load default class: " + defaultClass);
			}

			return forName(defaultClass, false);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> Class<T> forName(String className, boolean initialize, ClassLoader classLoader) throws ClassNotFoundException {
		// first try to find the class
		try {
			className = getDecryptedClassName(className);

			try {
				return (Class<T>) Class.forName(className, initialize, classLoader);
			} catch (ClassNotFoundException ex) {
				return RefactorClassRegistry.getInstance().findClass(className, ex);
			}
		} catch (Exception e) {
			throw new ClassNotFoundException("Class with name " + className + " can not be found. Class loader: " + classLoader, e);
		}
	}

	public static RefactorClassRegistry getInstance(){
		return (RefactorClassRegistry) SingletonRepository.getRepository().getInstance(RefactorClassRegistry.class, instantiator);
	}

	private Map<String, String> refactoredClassNamesMap;
	private Map<String, String> refactoredPackageNamesMap;

	/**
	 * @return
	 */
	public Map<String, String> getRefactoredClassNames() {
		if(this.refactoredClassNamesMap==null){
			this.refactoredClassNamesMap=new HashMap<String, String>();
		}
		return this.refactoredClassNamesMap;
	}

	public Map<String, String> getRefactoredPackageNames() {
		if (this.refactoredPackageNamesMap == null) {
			this.refactoredPackageNamesMap = new HashMap<String, String>();
		}
		return this.refactoredPackageNamesMap;
	}

	/**
	 * @return
	 */
	public String getRefactoredClassName(String oldClassName) {
		oldClassName = getDecryptedClassName(oldClassName);

		String result = getRefactoredClassNames().get(oldClassName);
		if (result == null) {
			String[] packageClass = StringHandler.splitOffPackageFromClassName(oldClassName);
			String newPackage = getRefactoredPackageNames().get(packageClass[0]);
			if (newPackage != null) {
				StringBuffer buffer = new StringBuffer(newPackage);
				buffer.append(CoreConstants.DOT).append(packageClass[1]);
				return buffer.toString();
			}
		}
		return result;
	}

	public void registerRefactoredPackage(String oldPackageName, Package validPackage) {
		registerRefactoredPackage(oldPackageName, validPackage.getName());
	}

	public void registerRefactoredPackage(String oldPackageName, String validPackageName) {
		getRefactoredPackageNames().put(oldPackageName, validPackageName);
	}

	public void registerRefactoredClass(String oldClassName, Class<?> validClass) {
		registerRefactoredClass(oldClassName, validClass.getName());
	}

	public void registerRefactoredClass(String oldClassName,String newClassName){
		getRefactoredClassNames().put(oldClassName,newClassName);
	}

	public boolean isClassRefactored(String oldClassName){
		return getRefactoredClassName(oldClassName)!=null;
	}

	public Object newInstance(String className, Class<?> callerClass) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Class<?> myClass = classForName(className);
		return newInstance(myClass, callerClass);

	}

	public Object newInstance(Class<?> aClass, Class<?> callerClass) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		if (aClass.isInterface()) {
			return ImplementorRepository.getInstance().newInstance(aClass, callerClass);
		}
		return aClass.newInstance();
	}

	public <T> Class<T> classForName(String className) throws ClassNotFoundException {
		return RefactorClassRegistry.forName(className);
	}

	private <T> Class<T> findClass(String className, ClassNotFoundException classNotFoundEx) throws ClassNotFoundException {
		// bad luck
		// is the class refactored?
		String refactoredClassName = getRefactoredClassName(className);
		if (refactoredClassName == null) {
			// nothing found, throw exception
			throw classNotFoundEx;
		}
		// something was found...but does the class exist?
		try {
			@SuppressWarnings("unchecked")
			Class<T> theClass = (Class<T>) Class.forName(refactoredClassName);
			return theClass;
		} catch (ClassNotFoundException refactoredClassNotFoundEx) {
			// that is really bad luck (and strange)
			throw new ClassNotFoundException("[RefactorClassRegistry] Refactored class ( "+ refactoredClassName+" ) was not found. Original class name: "+className, classNotFoundEx);
		}
	}
}
