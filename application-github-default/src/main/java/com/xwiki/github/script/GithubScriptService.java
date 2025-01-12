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
package com.xwiki.github.script;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import com.xwiki.github.internal.configuration.GithubConfiguration;
import com.xwiki.github.internal.configuration.GithubManager;

import javax.inject.Named;
import javax.inject.Singleton;

@Component
@Named("github")
@Unstable
@Singleton
public class GithubScriptService implements ScriptService
{

    @Inject
    private GithubConfiguration configuration;

    @Inject
    private GithubManager manager;

    //TODO remove before release

    public GithubConfiguration getConfigurationObject()
    {
        return configuration;
    }

    public Map<String, List<List<String>>> execute(String account, String repo, String milestone) throws IOException
    {
        return manager.getMilestoneDetails(account, repo, milestone);
    }

    public List<String> getGithubAccounts(){
        return configuration.getGithubAccounts();
    }
}
