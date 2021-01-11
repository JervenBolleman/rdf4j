/** *****************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ****************************************************************************** */
package org.eclipse.rdf4j.sail.memory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.junit.BeforeClass;
import org.junit.Test;

public class MemorySparqlSimpleTest {

	private static SailRepository repository;

	@BeforeClass
	public static void beforeClass() throws IOException, InterruptedException {

		repository = new SailRepository(new MemoryStore());

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			Resource[] contexts = new Resource[] {
					connection.getValueFactory().createIRI("http://example.org/conexts/", "0"),
					connection.getValueFactory().createIRI("http://example.org/conexts/", "1")
			};
			for (int i = 0; i < SIZE; i++) {
				final IRI subject = connection.getValueFactory()
						.createIRI("http://example.org/subject/", String.valueOf(i));
				final IRI predicate = RDF.TYPE;
				final Literal object = connection.getValueFactory().createLiteral(i);
				final Resource context = contexts[i % 2];
				connection.add(connection.getValueFactory().createStatement(subject, predicate, object), context);
			}
			connection.commit();
		}

	}

	private static final int SIZE = 100_000;
//	private static final int SIZE = 1_000_000;

	@Test
	public void getAllTypesStatementSparql() {

		try (SailRepositoryConnection connection = repository.getConnection()) {
			for (int i = 0; i < 10; i++) {
				long count = 0;
				try (TupleQueryResult stream = connection
						.prepareTupleQuery("select * where {[] a ?c}")
						.evaluate()) {
					while (stream.hasNext()) {
						stream.next();
						count++;
					}
				}
				assertEquals(SIZE, count);
			}
		}
	}

	@Test
	public void groupByAllTypesStatementSparql() {

		try (SailRepositoryConnection connection = repository.getConnection()) {
			for (int i = 0; i < 1; i++) {
				long count = 0;
				try (TupleQueryResult stream = connection
						.prepareTupleQuery("select ?t (count(?s) as ?ss) where {?s a ?t} group by ?t")
						.evaluate()) {
					while (stream.hasNext()) {
						stream.next();
						count++;
					}
				}
				assertTrue(count > 10);
			}
		}
	}
}
