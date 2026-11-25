package com.ourgalaxy.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
@Transactional
class SpaceIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRejectCrossSpaceMemoryAndFileAccessAndRevokeLogoutToken() throws Exception {
        String ownerA = registerAndLogin("owner-a");
        String partnerA = registerAndLogin("partner-a");
        String ownerB = registerAndLogin("owner-b");
        String userC = registerAndLogin("user-c");

        long spaceA = createSpace(ownerA, "我们的宇宙 A");
        String invitationA = createInvitation(ownerA);
        MvcResult repeatedInvitation = mockMvc.perform(post("/api/spaces/invitations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerA)))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(invitationA, readData(repeatedInvitation).path("code").asText());
        long joinedSpaceA = joinSpace(partnerA, invitationA);
        assertEquals(spaceA, joinedSpaceA);

        long spaceB = createSpace(ownerB, "我们的宇宙 B");
        String invitationB = createInvitation(ownerB);
        assertEquals(spaceB, joinSpace(userC, invitationB));

        MockMultipartFile image = pngImage();
        MvcResult uploadResult = mockMvc.perform(multipart("/api/files")
                        .file(image)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mimeType").value("image/png"))
                .andReturn();
        long fileA = readData(uploadResult).path("id").asLong();

        long memoryA = createMemory(ownerA, "只属于 A 的回忆", fileA);

        mockMvc.perform(get("/api/memories/{id}", memoryA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/memories/{id}", memoryA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(memoryRequest("越权修改", null))))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/memories/{id}", memoryA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/files/{id}", fileA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/memories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(memoryRequest("不能引用 A 的图片", fileA))))
                .andExpect(status().isBadRequest());

        MvcResult listResult = mockMvc.perform(get("/api/memories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerB)))
                .andExpect(status().isOk())
                .andReturn();
        assertFalse(containsMemoryId(listResult, memoryA));

        mockMvc.perform(get("/api/memories/{id}", memoryA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(partnerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.media[0].file.id").value(fileA));
        mockMvc.perform(get("/api/files/{id}", fileA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerA)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));

        mockMvc.perform(put("/api/memories/{id}", memoryA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(memoryRequest("A 的回忆（已更新）", fileA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.media.length()").value(1))
                .andExpect(jsonPath("$.data.tags.length()").value(2));
        mockMvc.perform(put("/api/memories/{id}", memoryA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(memoryRequestWithoutAssociations("A 的回忆（已更新）"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.media.length()").value(0))
                .andExpect(jsonPath("$.data.tags.length()").value(0));
        mockMvc.perform(put("/api/memories/{id}", memoryA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(memoryRequest("A 的回忆（已更新）", fileA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.media.length()").value(1))
                .andExpect(jsonPath("$.data.tags.length()").value(2));

        mockMvc.perform(post("/api/spaces/invitations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerA)))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/spaces/join")
                        .header(HttpHeaders.AUTHORIZATION, bearer(registerAndLogin("late-joiner")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("code", invitationA))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerA)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerA)))
                .andExpect(status().isUnauthorized());
    }

    private String registerAndLogin(String prefix) throws Exception {
        String email = register(prefix);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "TestPassword123!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        String token = readData(result).path("token").asText();
        assertTrue(token.startsWith("eyJ"));
        return token;
    }

    private String register(String prefix) throws Exception {
        String email = prefix + "-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "email", email,
                                "password", "TestPassword123!",
                                "nickname", prefix
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email));
        return email;
    }

    private long createSpace(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/spaces")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("name", name))))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result).path("id").asLong();
    }

    private String createInvitation(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/spaces/invitations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result).path("code").asText();
    }

    private long joinSpace(String token, String code) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/spaces/join")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("code", code))))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result).path("id").asLong();
    }

    private long createMemory(String token, String title, Long fileId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/memories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(memoryRequest(title, fileId))))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result).path("id").asLong();
    }

    private Map<String, Object> memoryRequest(String title, Long fileId) {
        return Map.of(
                "title", title,
                "content", "一段只属于两个人的回忆",
                "eventDate", "2026-11-25",
                "locationName", "地球",
                "starType", "CLASSIC",
                "tags", List.of("测试", "回忆"),
                "mediaFileIds", fileId == null ? List.of() : List.of(fileId)
        );
    }

    private Map<String, Object> memoryRequestWithoutAssociations(String title) {
        return Map.of(
                "title", title,
                "content", "一段只属于两个人的回忆",
                "eventDate", "2026-11-25",
                "starType", "CLASSIC",
                "tags", List.of(),
                "mediaFileIds", List.of()
        );
    }

    private MockMultipartFile pngImage() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0xFF3366);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "png", output));
        return new MockMultipartFile("file", "test.png", "image/png", output.toByteArray());
    }

    private JsonNode readData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private boolean containsMemoryId(MvcResult result, long memoryId) throws Exception {
        JsonNode data = readData(result);
        for (JsonNode memory : data) {
            if (memory.path("id").asLong() == memoryId) {
                return true;
            }
        }
        return false;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
