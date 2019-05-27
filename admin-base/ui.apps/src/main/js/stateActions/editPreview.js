/*-
 * #%L
 * admin base - UI Apps
 * %%
 * Copyright (C) 2017 headwire inc.
 * %%
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * #L%
 */
import { LoggerFactory } from '../logger'
let log = LoggerFactory.logger('editPreview').setLevelDebug()

import { set } from '../utils'

export default function(me, target) {

    log.fine(target);

    let view = me.getView();

    if (!view.state.tools.workspace) {
        set(view, '/state/tools/workspace', {});
    }

    if(target === 'preview') {
        if(view.state.tools.workspace.preview === 'preview') {
            set(view, '/state/tools/workspace/preview', '');
            set(view, '/pageView/view', view.state.tools.workspace.view)
        } else {
            set(view, '/state/tools/workspace/preview', target);
            set(view, '/pageView/view', target)
        }
    } else if (target === 'ignore-containers'){
        if(view.state.tools.workspace.ignoreContainers === 'ignore-containers') {
            set(view, '/state/tools/workspace/ignoreContainers', '');
        } else {
            set(view, '/state/tools/workspace/ignoreContainers', target);
        }
    } else {
        set(view, '/state/tools/workspace/view', target);
    }
}
