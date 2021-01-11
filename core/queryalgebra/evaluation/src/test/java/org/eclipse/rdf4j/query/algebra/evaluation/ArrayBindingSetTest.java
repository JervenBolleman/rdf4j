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

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.junit.Before;
import org.junit.Test;

public class ArrayBindingSetTest {

	private final MapBindingSet mbs = new MapBindingSet();
	private final ArrayBindingSet abs = new ArrayBindingSet("foo");

	private ValueFactory vf = SimpleValueFactory.getInstance();

	@Before
	public void setup() {
		abs.setBinding("foo", vf.createIRI("urn:foo"));
		mbs.addBinding("foo", vf.createIRI("urn:foo"));
	}

	@Test
	public void testEqualsMapBindingSet() {

		ArrayBindingSet bs = new ArrayBindingSet("foo");
		assertFalse(bs.equals(abs));
		assertFalse(bs.equals(mbs));

		bs.setBinding("foo", vf.createIRI("urn:foo"));

		assertEquals(bs, abs);
		assertEquals(bs, mbs);
		assertEquals(abs, mbs);
	}

	@Test
	public void testConstructor() {
		ArrayBindingSet bs = new ArrayBindingSet(mbs, "foo");
		assertTrue(bs.equals(mbs));
		assertTrue(mbs.equals(bs));
		assertEquals("objects that return true on their equals() method must have identical hash codes", bs.hashCode(),
				mbs.hashCode());
	}

	@Test
	public void testConstructor2() {
		ArrayBindingSet bs = new ArrayBindingSet(mbs, "bar");
		assertTrue(bs.equals(mbs));
		assertTrue(mbs.equals(bs));
		assertEquals("objects that return true on their equals() method must have identical hash codes", bs.hashCode(),
				mbs.hashCode());
	}

	@Test
	public void testConstructor3() {
		ArrayBindingSet bs = new ArrayBindingSet(mbs, "bar");
		bs.setBinding("bar", vf.createIRI("urn:bar"));
		assertFalse(bs.equals(mbs));
		assertFalse(mbs.equals(bs));
		assertNotEquals("objects that return true on their equals() method must have identical hash codes",
				bs.hashCode(),
				mbs.hashCode());
	}

	@Test
	public void testConstructor4() {
		ArrayBindingSet bs = new ArrayBindingSet(new EmptyBindingSet(), "bar");
		bs.setBinding("bar", vf.createIRI("urn:bar"));
		assertFalse(bs.equals(mbs));
		assertFalse(mbs.equals(bs));
		assertNotEquals("objects that return true on their equals() method must have identical hash codes",
				bs.hashCode(),
				mbs.hashCode());
	}

	@Test
	public void testConstructor5() {
		ArrayBindingSet bs = new ArrayBindingSet(abs, "bar");
		assertTrue(bs.equals(mbs));
		assertTrue(mbs.equals(bs));
		assertEquals("objects that return true on their equals() method must have identical hash codes", bs.hashCode(),
				mbs.hashCode());
	}

	@Test
	public void testHashcodeMapBindingSet() {
		assertTrue(abs.equals(mbs));
		assertTrue(mbs.equals(abs));
		assertEquals("objects that return true on their equals() method must have identical hash codes", abs.hashCode(),
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

		bs1.setBinding("x", RDF.ALT);
		bs1.setBinding("y", RDF.BAG);
		bs1.setBinding("z", RDF.FIRST);

		bs2.setBinding("y", RDF.BAG);
		bs2.setBinding("x", RDF.ALT);
		bs2.setBinding("z", RDF.FIRST);
		assertEquals(bs1, bs2);
		assertEquals(bs1.hashCode(), bs2.hashCode());
	}

}
