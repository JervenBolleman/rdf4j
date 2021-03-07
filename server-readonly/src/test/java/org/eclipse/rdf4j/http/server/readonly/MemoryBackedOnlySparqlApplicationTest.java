package org.eclipse.rdf4j.http.server.readonly;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(MemoryBackedOnlySparqlApplicationTestConfig.class)
public class MemoryBackedOnlySparqlApplicationTest {
	

	@Autowired
	private QueryResponder queryResponder;

	@Test
	public void contextLoads() {
		assertThat(queryResponder).isNotNull();
	}
}
