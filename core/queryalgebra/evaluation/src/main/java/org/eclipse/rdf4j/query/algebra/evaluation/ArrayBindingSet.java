/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation;

import java.util.Arrays;
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

	private String[] bindingNames;

	private Value[] values;

	/**
	 * Creates a new Array-based BindingSet for the supplied bindings names. <em>The supplied list of binding names is
	 * assumed to be constant</em>; care should be taken that the contents of this array doesn't change after supplying
	 * it to this solution.
	 *
	 * @param names The binding names.
	 */
	public ArrayBindingSet(String... names) {
		if (names.length == 0) {
			this.bindingNames = new String[4];
			this.values = new Value[4];
		} else {
			this.bindingNames = names;
			this.values = new Value[names.length];
		}
	}

	public ArrayBindingSet(BindingSet toCopy, String... names) {
		// Names might already be present in the toCopy BindingSet.
		// This is ok, because we always take the first name/value pair.
		// However we do need to make sure that we don't accidentally set
		// the value for the duplicated name.
		if (toCopy instanceof ArrayBindingSet) {
			ArrayBindingSet abs = (ArrayBindingSet) toCopy;
			this.bindingNames = Arrays.copyOf(abs.bindingNames, abs.bindingNames.length + names.length);
			System.arraycopy(names, 0, bindingNames, abs.bindingNames.length, names.length);
			this.values = Arrays.copyOf(abs.values, abs.bindingNames.length + names.length);
		} else {
			Set<String> bindingNamesToCopy = toCopy.getBindingNames();
			final int toCopySize = bindingNamesToCopy.size();
//			assert toCopySize == bindingNamesToCopy.size() : toCopy.getClass()
//					+ " has more names than it's size suggests";
			this.bindingNames = new String[toCopySize + names.length];
			final Iterator<String> iter = bindingNamesToCopy.iterator();
			for (int i = 0; iter.hasNext(); i++) {
				this.bindingNames[i] = iter.next();
			}
			System.arraycopy(names, 0, bindingNames, toCopySize, names.length);
			this.values = new Value[bindingNames.length];
			for (int i = 0; i < toCopySize; i++) {
				this.values[i] = toCopy.getValue(bindingNames[i]);
			}
		}
	}

	public ArrayBindingSet(ArrayBindingSet toCopy, String... names) {
		this.bindingNames = Arrays.copyOf(toCopy.bindingNames, toCopy.bindingNames.length + names.length);
		System.arraycopy(names, 0, bindingNames, toCopy.bindingNames.length, names.length);
		this.values = Arrays.copyOf(toCopy.values, toCopy.bindingNames.length + names.length);
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
			if (bindingNames[i] != null && bindingNames[i].equals(bindingName)) {
				final int idx = i;
				return (a, v) -> a.values[idx] = v;
			}
		}
		return null;
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
			if (bindingNames[i] != null && values[i] != null)
				bns.add(bindingNames[i]);
		}
		return bns;
	}

	@Override
	public Value getValue(String bindingName) {
		for (int i = 0; i < bindingNames.length; i++) {
			if (bindingNames[i] != null && bindingNames[i].equals(bindingName))
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

	public void addAll(BindingSet bindingSet) {
		for (Binding binding : bindingSet) {
			this.addBinding(binding.getName(), binding.getValue());
		}
	}

	public int setBinding(String bindingName, Value value) {
		for (int i = 0; i < this.bindingNames.length; i++) {
			if (this.bindingNames[i] == bindingName) {
				this.values[i] = value;
				return i;
			}
		}

		int lastnull = -(this.bindingNames.length + 1);
		for (int i = 0; i < this.bindingNames.length; i++) {
			if (this.bindingNames[i] == null)
				lastnull = -(i + 1);
			else if (this.bindingNames[i].equals(bindingName)) {
				this.values[i] = value;
				return i;
			}
		}
		return lastnull;
	}

	public void addBinding(String bindingName, Value value) {
		int oldLength = this.bindingNames.length;
		int set = setBinding(bindingName, value);
		if (set == -oldLength - 1) {
			this.bindingNames = Arrays.copyOf(this.bindingNames, oldLength + 1);
			this.values = Arrays.copyOf(this.values, this.values.length + 1);
			this.bindingNames[oldLength] = bindingName;
			this.values[oldLength] = value;
		} else if (set < 0) {
			this.bindingNames[Math.abs(set + 1)] = bindingName;
			this.values[Math.abs(set + 1)] = value;
		}
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

	public void removeBinding(String string) {
		for (int i = 0; i < bindingNames.length; i++) {
			if (string.equals(bindingNames[i])) {
				bindingNames[i] = null;
				values[i] = null;
			}
		}

	}
}
