/*******************************************************************************
 * Copyright (c) 2012 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package fr.obeo.uml2.debugtools.views;


import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.uml2.common.util.CacheAdapter;

/**
 * @author <a href="mailto:mikael.barbero@obeo.fr">Mikael Barbero</a>
 */
public class CacheAdapterSizeView extends ViewPart {

	private Label sizeDisplay;
	private Label timeDisplay;
	private Map<Resource, Map<EObject, Map<Object, Object>>> cacheAdapterValues;
	private CacheAdapterSizeReaderAndUIUpdater cacheAdapterSizeReaderAndUIUpdateThread;

	@SuppressWarnings("unchecked")
	public void createPartControl(Composite parent) {
		timeDisplay = new Label(parent, SWT.NONE);
		sizeDisplay = new Label(parent, SWT.NONE);
		
		Field field = getField(CacheAdapter.class, "values");
		try {
			cacheAdapterValues = (Map<Resource, Map<EObject, Map<Object, Object>>>) field.get(CacheAdapter.getInstance());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		cacheAdapterSizeReaderAndUIUpdateThread = new CacheAdapterSizeReaderAndUIUpdater();
		cacheAdapterSizeReaderAndUIUpdateThread.start();
	}

	@Override
	public void dispose() {
		cacheAdapterSizeReaderAndUIUpdateThread.doStop();
		super.dispose();
	}
	
	private static Field getField(Class<?> clazz, String fieldName) {
		try {
			Field declaredField = clazz.getDeclaredField(fieldName);
			makeAccessible(declaredField);
			return declaredField;
		} catch (NoSuchFieldException e) {
			Class<?> superClass = clazz.getSuperclass();
			if (superClass == null) {
				throw new RuntimeException(e);
			} else {
				return getField(superClass, fieldName);
			}
		}
	}

	private static void makeAccessible(Member member) {
		if (!Modifier.isPublic(member.getModifiers())
				|| !Modifier.isPublic(member.getDeclaringClass().getModifiers())) {
			if (member instanceof AccessibleObject) {
				((AccessibleObject)member).setAccessible(true);
			}
		}
	}
	
	/**
	 * @author <a href="mailto:mikael.barbero@obeo.fr">Mikael Barbero</a>
	 *
	 */
	private final class CacheAdapterSizeReaderAndUIUpdater extends Thread {
		private volatile boolean isStopped;

		/** 
		 * {@inheritDoc}
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			while(!isStopped) {
				int size = cacheAdapterValues.size();
				synchronized (cacheAdapterValues) {
					for (Map<EObject, Map<Object, Object>> map1 : cacheAdapterValues.values()) {
						size += map1.size();
						synchronized (map1) {
							for (Map<Object, Object> map2 : map1.values()) {
								size += map2.size();
							}
						}
					}
				}
				final int finalSize = size;
				getSite().getShell().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						timeDisplay.setText(Calendar.getInstance().getTime().toString());
						sizeDisplay.setText("CacheAdapter.getInstance() size = " + finalSize);
					}
				});
				
				try {
					Thread.sleep(TimeUnit.MILLISECONDS.convert(2L, TimeUnit.SECONDS));
				} catch (InterruptedException e) {
					Thread.interrupted();
				}
			}
		}
		
		void doStop() {
			isStopped = true;
		}
	}

	/** 
	 * {@inheritDoc}
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		sizeDisplay.setFocus();
	}


}