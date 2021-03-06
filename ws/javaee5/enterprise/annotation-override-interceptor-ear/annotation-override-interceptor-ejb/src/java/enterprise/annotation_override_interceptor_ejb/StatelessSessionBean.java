/*
 	Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
	
	This program and the accompanying materials are made available under the
	terms of the Eclipse Public License v. 2.0, which is available at
	http://www.eclipse.org/legal/epl-2.0.
	
	This Source Code may also be made available under the following Secondary
	Licenses when the conditions for such availability set forth in the
	Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
	version 2 with the GNU Classpath Exception, which is available at
	https://www.gnu.org/software/classpath/license.html.
	
	SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
*/
package enterprise.annotation_override_interceptor_ejb;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

// This bean uses two interceptors to validate
// the input to its (only) business method.
// Note that a single interceptor would suffice
// but to demonstrate the use of interceptor
// chaining, we use two interceptors

@Stateless
@Interceptors({ArgumentsChecker.class})
public class StatelessSessionBean
    implements StatelessSession {

    private static final String KEY = "interceptorNameList";

    private static Map<String, List<String>> interceptorNamesForMethod
            = new HashMap<String, List<String>>();

    //The bean interceptor method just caches the interceptor names
    //  in a map which is queried in the getInterceptorNamesFor() method
    @AroundInvoke
    private Object intercept(InvocationContext invCtx)
    	throws Exception {
	System.out.println("Entered aroundInvoke for Bean");
        Map<String, Object> ctxData = invCtx.getContextData();
        List<String> interceptorNameList = (List<String>) ctxData.get(KEY);
        if (interceptorNameList == null) {
            interceptorNameList = new ArrayList<String>();
            ctxData.put(KEY, interceptorNameList);
        }

        //Add this interceptor also to the list of interceptors invoked!!
        interceptorNameList.add("StatelessSessionBean");
        
        //Cache the interceptor name list in a map that can be queried later
        String methodName = invCtx.getMethod().getName();
	synchronized (interceptorNamesForMethod) {
            interceptorNamesForMethod.put(methodName, interceptorNameList);
	}

	return invCtx.proceed();
    }

    // This business method is called after the interceptor methods.
    // Hence it is guaranteed that the argument to this method is not null
    // and it starts with a letter
    public String initUpperCase(String val) {
        String first = val.substring(0, 1);
        return first.toUpperCase() + val.substring(1);
    }

    // This business method is called after the interceptor methods.
    // Hence it is guaranteed that the argument to this method is not null
    // and it starts with a letter
    public String initLowerCase(String val) {
        String first = val.substring(0, 1);
        return first.toLowerCase() + val.substring(1);
    }

    //Note:-
    //  Since this method takes a int as a parameter, the ArgumentChecker
    //  inteceptor is disabled in the ejb-jar.xml for this method.
    public boolean isOddNumber(int val) {
        return ((val % 2) != 0);
    }

    /**
     * Only the default interceptor is used to intercept this method
     */
    public List<String> getInterceptorNamesFor(String methodName) {
        return interceptorNamesForMethod.get(methodName);
    }

}
