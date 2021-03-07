package org.eclipse.rdf4j.http.server.readonly;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class MemoryBackedOnlySparqlApplicationTestConfig {

	@Bean
	public Repository getTestRepository() {
		return new SailRepository(new MemoryStore());
	}
}
