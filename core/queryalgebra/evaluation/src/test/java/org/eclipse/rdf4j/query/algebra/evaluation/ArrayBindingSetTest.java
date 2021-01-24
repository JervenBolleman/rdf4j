/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.Set;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.junit.Before;
import org.junit.Test;

public class ArrayBindingSetTest {

	private final MapBindingSet mbs = new MapBindingSet();
	private final ArrayBindingSet qbs = new ArrayBindingSet("foo");

	private ValueFactory vf = SimpleValueFactory.getInstance();

	@Before
	public void setup() {
		qbs.getDirectSetterForVariable("foo").accept(qbs, vf.createIRI("urn:foo"));
		mbs.addBinding("foo", vf.createIRI("urn:foo"));
	}

	@Test
	public void testEqualsMapBindingSet() {

		ArrayBindingSet bs = new ArrayBindingSet("foo");
		assertFalse(bs.equals(qbs));
		assertFalse(bs.equals(mbs));

		bs.getDirectSetterForVariable("foo").accept(bs, vf.createIRI("urn:foo"));

		assertEquals(bs, qbs);
		assertEquals(bs, mbs);
		assertEquals(qbs, mbs);
	}

	@Test
	public void testHashcodeMapBindingSet() {
		assertTrue(qbs.equals(mbs));
		assertTrue(mbs.equals(qbs));
		assertEquals("objects that return true on their equals() method must have identical hash codes", qbs.hashCode(),
				mbs.hashCode());
	}

	/**
	 * Verifies that the BindingSet implementation honors the API spec for {@link BindingSet#equals(Object)} and
	 * {@link BindingSet#hashCode()}.
	 */
	@Test
	public void testEqualsHashcode() {
		ArrayBindingSet bs1 = new ArrayBindingSet("x", "y", "z");
		ArrayBindingSet bs2 = new ArrayBindingSet("x", "y", "z");

		bs1.getDirectSetterForVariable("x").accept(bs1, RDF.ALT);
		bs1.getDirectSetterForVariable("y").accept(bs1, RDF.BAG);
		bs1.getDirectSetterForVariable("z").accept(bs1, RDF.FIRST);

		bs2.getDirectSetterForVariable("y").accept(bs2, RDF.BAG);
		bs2.getDirectSetterForVariable("x").accept(bs2, RDF.ALT);
		bs2.getDirectSetterForVariable("z").accept(bs2, RDF.FIRST);
		assertEquals(bs1, bs2);
		assertEquals(bs1.hashCode(), bs2.hashCode());
	}

	@Test
	public void testAddAll() {
		{
			ArrayBindingSet bs = new ArrayBindingSet();
			bs.addAll(new ArrayBindingSet());
			assertEquals(0, bs.size());
		}
		{
			ArrayBindingSet bs = new ArrayBindingSet();
			bs.addBinding("foo", vf.createIRI("urn:foo"));
			bs.addAll(new ArrayBindingSet());
			assertEquals(1, bs.size());
		}
		{
			ArrayBindingSet bs = new ArrayBindingSet();
			bs.addBinding("foo", vf.createIRI("urn:foo"));
			final ArrayBindingSet bs2 = new ArrayBindingSet();
			bs2.addBinding("foo", vf.createIRI("urn:foo"));
			bs.addAll(bs2);
			assertEquals(1, bs.size());
		}
		{
			ArrayBindingSet bs = new ArrayBindingSet();
			bs.addBinding("foo", vf.createIRI("urn:foo"));
			final ArrayBindingSet bs2 = new ArrayBindingSet();
			bs2.addBinding("foo", vf.createIRI("urn:foo"));
			bs2.addBinding("bar", vf.createIRI("urn:bar"));
			bs.addAll(bs2);
			assertEquals(2, bs.size());
		}
	}

	@Test
	public void testAdd() {
		try {
			ArrayBindingSet bs = new ArrayBindingSet();
			bs.addBinding("foo", vf.createIRI("urn:foo"));
			bs.addBinding("foo", vf.createIRI("urn:foo"));
			fail();
		} catch (AssertionError e) {
			// The current implementation sets an assertion.
			// however the behavior that is expected is that
			// after adding an existing binding only the last
			// set one is returned.
			return;
		}
	}

	@Test
	public void testNonSharedBackingMap() {
		ArrayBindingSet bs = new ArrayBindingSet();
		bs.addBinding("foo", vf.createIRI("urn:foo"));
		bs.addBinding("bar", vf.createIRI("urn:bar"));
		ArrayBindingSet bs2 = new ArrayBindingSet(bs);
		bs2.addBinding("boo", vf.createIRI("urn:boo"));
		assertNotEquals(bs.size(), bs2.size());
		assertFalse(bs.hasBinding("boo"));
	}

	@Test
	public void testSet() {
		ArrayBindingSet bs = new ArrayBindingSet();
		bs.addBinding("foo", vf.createIRI("urn:foo"));
		bs.setBinding("foo", vf.createIRI("urn:foo2"));
		assertEquals(1, bs.size());
		assertEquals(vf.createIRI("urn:foo2"), bs.getBinding("foo").getValue());
	}

	@Test
	public void testRemove() {
		ArrayBindingSet bs = new ArrayBindingSet();
		bs.addBinding("foo", vf.createIRI("urn:foo"));
		bs.removeBinding("foo");
		assertEquals(0, bs.size());
		bs = new ArrayBindingSet();
		bs.addBinding("foo", vf.createIRI("urn:foo"));
		bs.removeBinding("bar");
		assertEquals(1, bs.size());
	}

	@Test
	public void testBindingNames() {
		ArrayBindingSet bs = new ArrayBindingSet();
		for (int i = 0; i < 128; i++) {
			final String name = String.valueOf(i);
			bs.addBinding(name, RDF.ALT);
		}
		final Set<String> bN = bs.getBindingNames();
		for (int i = 0; i < 128; i++) {
			final String name = String.valueOf(i);
			assertTrue(bN.contains(name));
		}
		assertEquals(128, bs.size());
	}

	@Test
	public void testBindings() {
		ArrayBindingSet bs = new ArrayBindingSet();
		for (int i = 0; i < 128; i++) {
			final String name = String.valueOf(i);
			bs.addBinding(name, RDF.ALT);
		}
		Iterator<Binding> iter = bs.iterator();
		for (int i = 0; i < 128; i++) {
			assertTrue(iter.hasNext());
			assertEquals(RDF.ALT, iter.next().getValue());
		}
		assertFalse(iter.hasNext());
	}

	@Test
	public void testGetValue() {
		ArrayBindingSet bs = new ArrayBindingSet();
		for (int i = 0; i < 128; i++) {
			final String name = String.valueOf(i);
			bs.addBinding(name, RDF.ALT);
		}
		for (int i = 0; i < 128; i++) {
			final String name = String.valueOf(i);
			assertEquals(RDF.ALT, bs.getValue(name));
		}
		assertEquals(128, bs.size());
	}
}
