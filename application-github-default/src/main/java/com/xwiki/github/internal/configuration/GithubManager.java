/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xwiki.github.internal.configuration;

import java.io.IOException;
import java.net.URI;
import java.util.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.UriBuilder;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.stability.Unstable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component(roles = GithubManager.class)
@Singleton
@Unstable
public class GithubManager {

    private static final String GITHUB_URL = "https://api.github.com";
    private static final List<String> ISSUES_CATEGORIES = Arrays.asList("BUG", "NEW", "IMPROVEMENT");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Inject
    private HttpClientBuilderFactory httpClientBuilderFactory;

    public Map<String, List<List<String>>> getMilestoneDetails(String account, String repo, String milestone) throws IOException {
        try (CloseableHttpClient client = httpClientBuilderFactory.create()) {
            HttpGet getMilestones = new HttpGet(buildURI(account, repo, "milestones", Map.of("state", "all",
                "per_page", "100")));
            try (CloseableHttpResponse response = client.execute(getMilestones)) {
                JsonNode milestoneJson = getMilestone(EntityUtils.toString(response.getEntity()), milestone);
                HttpGet getIssues = new HttpGet(buildURI(account, repo, "issues", Map.of("milestone", milestoneJson.get("number").asText(), "state", "all")));
                try (CloseableHttpResponse issuesResponse = client.execute(getIssues)) {
                    return parseIssues(EntityUtils.toString(issuesResponse.getEntity()));
                }
            }
        }
    }

    private Map<String, List<List<String>>> parseIssues(String jsonString) throws JsonProcessingException {
        Map<String, List<List<String>>> issuesMap = new HashMap<>();
        ISSUES_CATEGORIES.forEach(category -> issuesMap.put(category, new ArrayList<>()))
        ;

        JsonNode issuesJson = OBJECT_MAPPER.readTree(jsonString);
        for (JsonNode issue : issuesJson) {
            String category = determineIssueCategory(issue.get("labels"));
            issuesMap.get(category).add(Arrays.asList(
                issue.get("title").asText(),
                issue.get("number").asText(),
                issue.get("url").asText()
            ));
        }
        return issuesMap;
    }

    private String determineIssueCategory(JsonNode labelsNode) {
        for (JsonNode label : labelsNode) {
            String labelName = label.get("name").asText().toLowerCase();
            return ISSUES_CATEGORIES.stream()
                .filter(category -> labelName.contains(category.toLowerCase()))
                .findFirst()
                .orElse("MISC");
        }
        return "MISC";
    }

    private JsonNode getMilestone(String jsonString, String milestone) throws JsonProcessingException {
        JsonNode milestonesJson = OBJECT_MAPPER.readTree(jsonString);
        for (JsonNode milestoneNode : milestonesJson) {
            if (milestoneNode.get("title").asText().equals(milestone)) {
                return milestoneNode;
            }
        }
        throw new IllegalArgumentException("Milestone not found: " + milestone);
    }

    private URI buildURI(String account, String repo, String endpoint, Map<String, String> queryParams) {
        UriBuilder uriBuilder = UriBuilder.fromUri(GITHUB_URL)
            .path("repos")
            .path(account)
            .path(repo)
            .path(endpoint);

        queryParams.forEach(uriBuilder::queryParam);
        return uriBuilder.build();
    }
}
