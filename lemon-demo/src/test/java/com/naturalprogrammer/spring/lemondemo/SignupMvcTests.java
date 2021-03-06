package com.naturalprogrammer.spring.lemondemo;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;

import com.guedim.spring.lemondemo.domain.User;
import com.naturalprogrammer.spring.lemon.security.LemonSecurityConfig;
import com.naturalprogrammer.spring.lemon.util.LemonUtils;

@Sql({"/test-data/initialize.sql", "/test-data/finalize.sql"})
public class SignupMvcTests extends AbstractMvcTests {
	
	@Test
	public void testSignupWithInvalidData() throws Exception {
		
		User invalidUser = new User("abc", "user1", null);

		mvc.perform(post("/api/core/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content(LemonUtils.toJson(invalidUser)))
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.errors[*].field").value(hasSize(4)))
				.andExpect(jsonPath("$.errors[*].field").value(hasItems(
					"user.email", "user.password", "user.name")));
		
		verify(mailSender, never()).send(any());
	}

	@Test
	public void testSignup() throws Exception {
		
		User user = new User("user.foo@example.com", "user123", "User Foo");

		mvc.perform(post("/api/core/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content(LemonUtils.toJson(user)))
				.andExpect(status().is(201))
				.andExpect(header().string(LemonSecurityConfig.TOKEN_RESPONSE_HEADER_NAME, containsString(".")))
				.andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.username").value("user.foo@example.com"))
				.andExpect(jsonPath("$.roles").value(hasSize(1)))
				.andExpect(jsonPath("$.roles[0]").value("UNVERIFIED"))
				.andExpect(jsonPath("$.tag.name").value("User Foo"))
				.andExpect(jsonPath("$.unverified").value(true))
				.andExpect(jsonPath("$.blocked").value(false))
				.andExpect(jsonPath("$.admin").value(false))
				.andExpect(jsonPath("$.goodUser").value(false))
				.andExpect(jsonPath("$.goodAdmin").value(false));
				
		verify(mailSender).send(any());

		// Ensure that password got encrypted
		Assert.assertNotEquals("user123", userRepository.findByEmail("user.foo@example.com").get().getPassword());
	}
	
//	@Test
//	public void testSignupLoggedIn() throws Exception {
//		
//		String adminToken = login("admin@example.com", "admin!");
//
//		User user = new User("user1@example.com", "user123", "User 1");
//
//		mvc.perform(post("/api/core/users")
//				.header(LemonSecurityConfig.TOKEN_REQUEST_HEADER_NAME, adminToken)
//				.contentType(MediaType.APPLICATION_JSON)
//				.content(LemonUtils.toJson(user)))
//				.andExpect(status().is(403));
//	}
//	
	@Test
	public void testSignupDuplicateEmail() throws Exception {
		
		User user = new User("user@example.com", "user123", "User");

		mvc.perform(post("/api/core/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content(LemonUtils.toJson(user)))
				.andExpect(status().is(422));
		
		verify(mailSender, never()).send(any());
	}
}
