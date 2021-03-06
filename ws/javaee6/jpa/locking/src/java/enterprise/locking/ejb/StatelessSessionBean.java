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

package enterprise.locking.ejb;

import enterprise.locking.entity.Part;
import enterprise.locking.entity.User;

import javax.ejb.Stateless;
import javax.persistence.*;
import java.util.Random;

@Stateless
public class StatelessSessionBean {

    @PersistenceContext
    private EntityManager em;

    private String name = "foo";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Initializes data for given number of consumers, suppliers and parts
     */
    public void initData(int numberOfConsumers, int numberOfSuppliers, int numberOfParts) {
        // user info
        int uid = 0;
        String fName = null;
        String lName = null;
        User u = null;
        int pid = 0;
        String pName = null;
        Part p = null;
        Part partRef[] = new Part[numberOfParts];

        for (int i = 0; i < numberOfParts; i++) {
            pid++;
            pName = "p" + pid;
            p = new Part(pid, pName);
            p.setAmount(500);
            partRef[i] = p;
            em.persist(p);
        }


        int partIndex = 1;
        Random randomGenerator = new Random();
        for (int i = 0; i < numberOfConsumers; i++) {
            uid++;
            fName = "f" + uid;
            lName = "l" + uid;
            u = new User(uid, fName, lName);
            u.setUserType(User.UserType.CONSUMER);
            u.setCount(2);
            // randam int from 0, ..., NT-1
            partIndex = randomGenerator.nextInt(numberOfParts);
            u.setPart(partRef[partIndex]);
            em.persist(u);
        }

        for (int i = 0; i < numberOfSuppliers; i++) {
            uid++;
            fName = "f" + uid;
            lName = "l" + uid;
            u = new User(uid, fName, lName);
            u.setUserType(User.UserType.SUPPLIER);
            u.setCount(10);
            partIndex = randomGenerator.nextInt(numberOfParts);
            u.setPart(partRef[partIndex]);
            em.persist(u);
        }
    }

    /**
     * A find followed by some think time, followed by update.
     */
    public boolean updateWithOptimisticLock(int uID, int s) {
        boolean updateSuccessfull = true;
        User u = em.find(User.class, uID);
        int pID = u.getPart().getId();
        Part p = em.find(Part.class, pID);

        // Simulate think time to allow parallel threads to find Usrs in parallel.
        simulateThinkTimeForSecond(s);
        int uCount = u.getCount();
        int pAmount = p.getAmount();
        // update part
        if (u.getUserType() == User.UserType.CONSUMER) {
            p.setAmount(pAmount - uCount);
        } else {
            p.setAmount(pAmount + uCount);
        }

        try {
            em.flush();
        } catch (OptimisticLockException e) {
            System.out.println("Got OptimisticLockException while updating with Optimistic Lock. " +
                    "The transaction will be rolled back");
            updateSuccessfull = false;
        } catch (PersistenceException e ) {
            System.out.println("Got Exception while updating with optimstic lock" + e);
            updateSuccessfull = false;
        }
        return updateSuccessfull;
    }

    /**
     * A find with pessimistic lock followed by some think time, followed by update.
     */
    public boolean updateWithPessimisticLock(int uID, int s) {
        boolean updateSuccessfull = true;

        User u = em.find(User.class, uID);
        int pID = u.getPart().getId();
        // Using Pessimistic lock to find the part object.
        Part p = em.find(Part.class, pID, LockModeType.PESSIMISTIC_WRITE);

        // Simulate think time to allow parallel threads to find in parallel.
        simulateThinkTimeForSecond(s);
        int uCount = u.getCount();
        int pAmount = p.getAmount();
        // update part
        if (u.getUserType() == User.UserType.CONSUMER) {
            p.setAmount(pAmount - uCount);
        } else {
            p.setAmount(pAmount + uCount);
        }

        try {
            em.flush();
        } catch (PersistenceException e) {
            updateSuccessfull = false;
        }
        return updateSuccessfull;

    }

    public void simulateThinkTimeForSecond(int sec) {
        //TODO check if sleep is allowed by EE spec
        try {
            Thread.sleep(sec * 1000);
        } catch (Exception ex) {
            System.out.println("get exp in sleep");
        }
    }

}
