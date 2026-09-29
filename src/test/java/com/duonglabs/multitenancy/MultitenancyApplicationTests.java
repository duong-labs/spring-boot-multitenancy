package com.duonglabs.multitenancy;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MultitenancyApplicationTests {

	@Autowired
	MockMvc mvc;

	@Test
	void eachTenantOnlySeesItsOwnData() throws Exception {
		createTenant("acme");
		createTenant("globex");

		checkIn("acme", "alice");
		checkIn("acme", "bob");
		checkIn("globex", "carol");

		mvc.perform(get("/check-ins").header("X-Tenant-Code", "acme"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].customerName", is("alice")));
		mvc.perform(get("/check-ins").header("X-Tenant-Code", "globex"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].customerName", is("carol")));
	}

	@Test
	void unknownOrMissingTenantIsRejected() throws Exception {
		mvc.perform(get("/check-ins").header("X-Tenant-Code", "nope")).andExpect(status().isNotFound());
		mvc.perform(get("/check-ins")).andExpect(status().isBadRequest());
	}

	@Test
	void duplicateTenantIsRejected() throws Exception {
		createTenant("dup");
		mvc.perform(post("/tenants").contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"dup\",\"name\":\"Dup\"}")).andExpect(status().isConflict());
	}

	private void createTenant(String code) throws Exception {
		mvc.perform(post("/tenants").contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"" + code + "\",\"name\":\"" + code + " inc\"}"))
				.andExpect(status().isCreated());
	}

	private void checkIn(String tenant, String customer) throws Exception {
		mvc.perform(post("/check-ins").header("X-Tenant-Code", tenant).contentType(MediaType.APPLICATION_JSON)
				.content("{\"customerName\":\"" + customer + "\"}"))
				.andExpect(status().isCreated());
	}
}
