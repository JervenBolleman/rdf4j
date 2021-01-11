/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiConsumer;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.AbstractBindingSet;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.impl.SimpleBinding;

/**
 * An array implementation of the {@link BindingSet} interface.
 *
 * @author Jerven Bolleman
 */
@InternalUseOnly
public class ArrayBindingSet extends AbstractBindingSet {

	private static final long serialVersionUID = -1L;

	private final String[] bindingNames;

	private final Value[] values;

	/**
	 * Creates a new Array-based BindingSet for the supplied bindings names. <em>The supplied list of binding names is
	 * assumed to be constant</em>; care should be taken that the contents of this array doesn't change after supplying
	 * it to this solution.
	 *
	 * @param names The binding names.
	 */
	public ArrayBindingSet(String... names) {
		this.bindingNames = names;
		this.values = new Value[names.length];
	}

	public ArrayBindingSet(BindingSet toCopy, String... names) {
		final HashSet<String> temp = new HashSet<String>(Arrays.asList(names));
		temp.addAll(toCopy.getBindingNames());
		this.bindingNames = temp.toArray(new String[0]);
		this.values = new Value[bindingNames.length];
		for (int i = 0; i < values.length; i++) {
			this.values[i] = toCopy.getValue(bindingNames[i]);
		}
	}

	public ArrayBindingSet(ArrayBindingSet toCopy, String... names) {
		this.bindingNames = Arrays.copyOf(toCopy.bindingNames, toCopy.bindingNames.length + names.length);
		assert this.bindingNames.length == (int) Arrays.stream(this.bindingNames).distinct().count();
		System.arraycopy(names, 0, bindingNames, bindingNames.length, names.length);
		this.values = Arrays.copyOf(toCopy.values, toCopy.bindingNames.length + names.length);
		;
	}

	/**
	 * This is used to generate a direct setter into the array to put a binding value into. Can be used to avoid many
	 * comparisons to the bindingNames.
	 * 
	 * @param bindingName for which you want the setter
	 * @return the setter biconsumer which can operate on any ArrayBindingSet but should only be used on ones with an
	 *         identical bindingNames array. Otherwise returns null.
	 */
	public BiConsumer<ArrayBindingSet, Value> getDirectSetterForVariable(String bindingName) {
		for (int i = 0; i < this.bindingNames.length; i++) {
			if (bindingNames[i].equals(bindingName)) {
				final int idx = i;
				return (a, v) -> a.values[idx] = v;
			}
		}
		return null;
	}

	public void setBinding(String bindingName, Value value) {
		// We first try to test for pointer identity as that is super fast
		// only if we can't find the bindingName because the string was not
		// reused do we try to use the equals method.
		for (int i = 0; i < this.bindingNames.length; i++) {
			if (bindingNames[i] == bindingName) {
				values[i] = value;
				return;
			}
		}
		for (int i = 0; i < this.bindingNames.length; i++) {
			if (bindingNames[i].equals(bindingName)) {
				values[i] = value;
				return;
			}
		}
	}

	public String nameByIndex(int i) {
		return bindingNames[i];
	}

	public Value valueByIndex(int i) {
		return values[i];
	}

	@Override
	public Set<String> getBindingNames() {
		final LinkedHashSet<String> bns = new LinkedHashSet<>();
		for (int i = 0; i < this.bindingNames.length; i++) {
			if (values[i] != null)
				bns.add(bindingNames[i]);
		}
		return bns;
	}

	@Override
	public Value getValue(String bindingName) {
		for (int i = 0; i < bindingNames.length; i++) {
			if (bindingNames[i].equals(bindingName))
				return values[i];
		}
		return null;
	}

	@Override
	public Binding getBinding(String bindingName) {
		Value value = getValue(bindingName);

		if (value != null) {
			return new SimpleBinding(bindingName, value);
		}

		return null;
	}

	@Override
	public boolean hasBinding(String bindingName) {
		return getValue(bindingName) != null;
	}

	@Override
	public Iterator<Binding> iterator() {
		return new ArrayBindingSetIterator();
	}

	@Override
	public int size() {
		int size = 0;

		for (Value value : values) {
			if (value != null) {
				size++;
			}
		}

		return size;
	}

	/*------------------------------------*
	 * Inner class ArrayBindingSetIterator *
	 *------------------------------------*/

	private class ArrayBindingSetIterator implements Iterator<Binding> {

		private int index = 0;

		public ArrayBindingSetIterator() {
		}

		@Override
		public boolean hasNext() {
			for (; index < values.length; index++) {
				if (values[index] != null) {
					return true;
				}
			}
			return false;
		}

		@Override
		public Binding next() {
			return new SimpleBinding(bindingNames[index], values[index++]);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
