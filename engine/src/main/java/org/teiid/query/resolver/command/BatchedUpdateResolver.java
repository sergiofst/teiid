/*
 * Copyright Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags and
 * the COPYRIGHT.txt file distributed with this work.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.teiid.query.resolver.command;

import org.teiid.api.exception.query.QueryMetadataException;
import org.teiid.api.exception.query.QueryResolverException;
import org.teiid.core.TeiidComponentException;
import org.teiid.query.metadata.TempMetadataAdapter;
import org.teiid.query.resolver.CommandResolver;
import org.teiid.query.resolver.QueryResolver;
import org.teiid.query.sql.lang.BatchedUpdateCommand;
import org.teiid.query.sql.lang.Command;


/** 
 * Resolver for BatchedUpdateCommands
 * @since 4.2
 */
public class BatchedUpdateResolver implements CommandResolver {
    
    public void resolveCommand(Command command, TempMetadataAdapter metadata, boolean resolveNullLiterals) 
        throws QueryMetadataException, QueryResolverException, TeiidComponentException {

        BatchedUpdateCommand batchedUpdateCommand = (BatchedUpdateCommand) command;
        
        for (Command subCommand : batchedUpdateCommand.getUpdateCommands()) {
            QueryResolver.setChildMetadata(subCommand, command);
            QueryResolver.resolveCommand(subCommand, metadata.getMetadata());
        }
    }

}
