package com.tigerbeetle;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.management.OperationsException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests using a tigerbeetle instance.
 */
public class IntegrationTest {

    private static Account account1;
    private static Account account2;

    static {
        account1 = new Account();
        account1.setId(UUID.randomUUID());
        account1.setUserData(UUID.randomUUID());
        account1.setLedger(720);
        account1.setCode(1);

        account2 = new Account();
        account2.setId(UUID.randomUUID());
        account2.setUserData(UUID.randomUUID());
        account2.setLedger(720);
        account2.setCode(2);
    }

    private static Account[] randomAccounts(int num) {

        var accounts = new Account[num];

        for (int i = 0; i < accounts.length; i++) {
            var account = new Account();
            account.setId(UUID.randomUUID());
            account.setUserData(UUID.randomUUID());
            account.setLedger(720);
            account.setCode(1);

            accounts[i] = account;
        }

        return accounts;
    }

    private static UUID[] accountsToUuids(Account[] accounts) {

        var uuids = new UUID[accounts.length];

        for (int i = 0; i < accounts.length; i++) {
            uuids[i] = accounts[i].getId();
        }

        return uuids;
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullReplicaAddresses() throws Throwable {

        try (var client = new Client(0, null)) {

        } catch (Throwable any) {
            throw any;
        }
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullElementReplicaAddresses() throws Throwable {

        var replicaAddresses = new String[] {"3001", null};
        try (var client = new Client(0, replicaAddresses)) {

        } catch (Throwable any) {
            throw any;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorEmptyReplicaAddresses() throws Throwable {

        var replicaAddresses = new String[0];
        try (var client = new Client(0, replicaAddresses)) {

        } catch (Throwable any) {
            throw any;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorEmptyStringReplicaAddresses() throws Throwable {

        var replicaAddresses = new String[] {"", "", ""};
        try (var client = new Client(0, replicaAddresses)) {

        } catch (Throwable any) {
            throw any;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidReplicaAddresses() throws Throwable {

        var replicaAddresses = new String[] {"127.0.0.1:99999"};
        try (var client = new Client(0, replicaAddresses)) {

        } catch (Throwable any) {
            throw any;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNegativeCluster() throws Throwable {

        var replicaAddresses = new String[] {"3001"};
        try (var client = new Client(-1, replicaAddresses)) {

        } catch (Throwable any) {
            throw any;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNegativeMaxConcurrency() throws Throwable {

        var replicaAddresses = new String[] {"3001"};
        var maxConcurrency = -1;
        try (var client = new Client(0, replicaAddresses, maxConcurrency)) {

        } catch (Throwable any) {
            throw any;
        }
    }

    @Test
    public void testCreateAccountsArray() throws Throwable {

        try (var server = new Server()) {
            try (var client = new Client(0, new String[] {Server.TB_PORT})) {

                final int numTries = 10;
                final int numAccounts = 1;

                for (int onTry = 0; onTry < numTries; onTry++) {
                    final var accounts = randomAccounts(numAccounts);
                    final var ids = accountsToUuids(accounts);

                    final var errors = client.createAccounts(accounts);
                    assertTrue(errors.length == 0);

                    final var lookupAccounts = client.lookupAccounts(ids);
                    for (int i = 0; i < accounts.length; i++) {
                        assertAccounts(accounts[i], lookupAccounts[i]);
                    }
                }
            } catch (Throwable any) {
                throw any;
            }

        } catch (Throwable any) {
            throw any;
        }
    }

    @Test
    public void testCreateAccountsBatch() throws Throwable {

        try (var server = new Server()) {
            try (var client = new Client(0, new String[] {Server.TB_PORT})) {

                var accountsBatch = new AccountsBatch(2);
                accountsBatch.add(account1);
                accountsBatch.add(account2);
                var errors = client.createAccounts(accountsBatch);
                assertTrue(errors.length == 0);

                var uuidsBatch = new UUIDsBatch(2);
                uuidsBatch.add(account1.getId());
                uuidsBatch.add(account2.getId());
                var lookupAccounts = client.lookupAccounts(uuidsBatch);
                assertAccounts(account1, lookupAccounts[0]);
                assertAccounts(account2, lookupAccounts[1]);

            } catch (Throwable any) {
                throw any;
            }

        } catch (Throwable any) {
            throw any;
        }
    }

    @Test
    public void testCreateSingleAccount() throws Throwable {

        try (var server = new Server()) {
            try (var client = new Client(0, new String[] {Server.TB_PORT})) {

                var error = client.createAccount(account1);
                assertTrue(error == CreateAccountResult.Ok);

                var lookupAccount = client.lookupAccount(account1.getId());
                assertNotNull(lookupAccount);
                assertAccounts(account1, lookupAccount);

            } catch (Throwable any) {
                throw any;
            }

        } catch (Throwable any) {
            throw any;
        }
    }

    @Test
    public void testCreateInvalidAccount() throws Throwable {

        try (var server = new Server()) {
            try (var client = new Client(0, new String[] {Server.TB_PORT})) {

                var account = new Account();
                var error = client.createAccount(account);
                assertTrue(error == CreateAccountResult.IdMustNotBeZero);

                var lookupAccount = client.lookupAccount(account.getId());
                assertNull(lookupAccount);

            } catch (Throwable any) {
                throw any;
            }

        } catch (Throwable any) {
            throw any;
        }
    }

    @Test
    public void testCreateAccountsAsyncArray() throws Throwable {

        try (var server = new Server()) {
            try (var client = new Client(0, new String[] {Server.TB_PORT})) {

                Future<CreateAccountsResult[]> createAccountsFuture =
                        client.createAccountsAsync(new Account[] {account1, account2});
                assertFalse(createAccountsFuture.isDone());

                var errors = createAccountsFuture.get();
                assertTrue(createAccountsFuture.isDone());
                assertTrue(errors.length == 0);

                Future<Account[]> lookupAccountsFuture =
                        client.lookupAccountsAsync(new UUID[] {account1.getId(), account2.getId()});
                assertFalse(lookupAccountsFuture.isDone());

                var lookupAccounts = lookupAccountsFuture.get();
                assertTrue(lookupAccountsFuture.isDone());
                assertAccounts(account1, lookupAccounts[0]);
                assertAccounts(account2, lookupAccounts[1]);

            } catch (Throwable any) {
                throw any;
            }

        } catch (Throwable any) {
            throw any;
        }
    }

    @Test
    public void testCreateAccountsAsyncBatch() throws Throwable {

        try (var server = new Server()) {
            try (var client = new Client(0, new String[] {Server.TB_PORT})) {

                var accountsBatch = new AccountsBatch(2);
                accountsBatch.add(account1);
                accountsBatch.add(account2);

                Future<CreateAccountsResult[]> createAccountsFuture =
                        client.createAccountsAsync(accountsBatch);
                assertFalse(createAccountsFuture.isDone());

                var errors = createAccountsFuture.get();
                assertTrue(createAccountsFuture.isDone());
                assertTrue(errors.length == 0);

                var uuidsBatch = new UUIDsBatch(2);
                uuidsBatch.add(account1.getId());
                uuidsBatch.add(account2.getId());

                Future<Account[]> lookupAccountsFuture = client.lookupAccountsAsync(uuidsBatch);
                assertFalse(lookupAccountsFuture.isDone());

                var lookupAccounts = lookupAccountsFuture.get();
                assertAccounts(account1, lookupAccounts[0]);
                assertAccounts(account2, lookupAccounts[1]);

            } catch (Throwable any) {
                throw any;
            }

        } catch (Throwable any) {
            throw any;
        }
    }

    @Test
    public void testCreateTransfersArray() throws Throwable {

        try (var server = new Server()) {
            try (var client = new Client(0, new String[] {Server.TB_PORT})) {

                var createAccountsErrors =
                        client.createAccounts(new Account[] {account1, account2});
                assertTrue(createAccountsErrors.length == 0);

                var transfer = new Transfer();
                transfer.setId(UUID.randomUUID());
                transfer.setCreditAccountId(account1.getId());
                transfer.setDebitAccountId(account2.getId());
                transfer.setLedger(720);
                transfer.setCode((short) 1);
                transfer.setAmount(100);

                var createTransfersErrors = client.createTransfers(new Transfer[] {transfer});
                assertTrue(createTransfersErrors.length == 0);

                var lookupAccounts =
                        client.lookupAccounts(new UUID[] {account1.getId(), account2.getId()});
                assertAccounts(account1, lookupAccounts[0]);
                assertAccounts(account2, lookupAccounts[1]);

                assertEquals(transfer.getAmount(), lookupAccounts[0].getCreditsPosted());
                assertEquals(0L, lookupAccounts[0].getDebitsPosted());

                assertEquals(transfer.getAmount(), lookupAccounts[1].getDebitsPosted());
                assertEquals(0L, lookupAccounts[1].getCreditsPosted());

                var lookupTransfers = client.lookupTransfers(new UUID[] {transfer.getId()});
                assertTrue(lookupTransfers.length == 1);

                assertTransfers(transfer, lookupTransfers[0]);
                assertNotEquals(0L, lookupTransfers[0].getTimestamp());

            } catch (Throwable any) {
                throw any;
            }

        } catch (Throwable any) {
            throw any;
        }
    }

    @Test
    public void testCreateTransfersBatch() throws Throwable {

        try (var server = new Server()) {
            try (var client = new Client(0, new String[] {Server.TB_PORT})) {

                var accountsBatch = new AccountsBatch(2);
                accountsBatch.add(account1);
                accountsBatch.add(account2);
                var createAccountErrors = client.createAccounts(accountsBatch);
                assertTrue(createAccountErrors.length == 0);

                var transfer = new Transfer();
                transfer.setId(UUID.randomUUID());
                transfer.setCreditAccountId(account1.getId());
                transfer.setDebitAccountId(account2.getId());
                transfer.setLedger(720);
                transfer.setCode((short) 1);
                transfer.setAmount(100);

                var transfersBatch = new TransfersBatch(1);
                transfersBatch.add(transfer);
                var createTransferErrors = client.createTransfers(transfersBatch);
                assertTrue(createTransferErrors.length == 0);

                var accountsUUIDsBatch = new UUIDsBatch(2);
                accountsUUIDsBatch.add(account1.getId());
                accountsUUIDsBatch.add(account2.getId());
                var lookupAccounts = client.lookupAccounts(accountsUUIDsBatch);
                assertAccounts(account1, lookupAccounts[0]);
                assertAccounts(account2, lookupAccounts[1]);

                assertEquals(transfer.getAmount(), lookupAccounts[0].getCreditsPosted());
                assertEquals(0L, lookupAccounts[0].getDebitsPosted());

                assertEquals(transfer.getAmount(), lookupAccounts[1].getDebitsPosted());
                assertEquals(0L, lookupAccounts[1].getCreditsPosted());

                var transfersUUIDsBatch = new UUIDsBatch(1);
                transfersUUIDsBatch.add(transfer.getId());
                var lookupTransfers = client.lookupTransfers(transfersUUIDsBatch);
                assertTrue(lookupTransfers.length == 1);

                assertTransfers(transfer, lookupTransfers[0]);
                assertNotEquals(0L, lookupTransfers[0].getTimestamp());

            } catch (Throwable any) {
                throw any;
            }

        } catch (Throwable any) {
            throw any;
        }
    }

    @Test
    public void testCreateTransfersAsyncArray() throws Throwable {

        try (var server = new Server()) {
            try (var client = new Client(0, new String[] {Server.TB_PORT})) {

                Future<CreateAccountsResult[]> createAccountsErrorsFuture =
                        client.createAccountsAsync(new Account[] {account1, account2});
                assertFalse(createAccountsErrorsFuture.isDone());

                var createAccountsErrors = createAccountsErrorsFuture.get();
                assertTrue(createAccountsErrors.length == 0);

                var transfer = new Transfer();
                transfer.setId(UUID.randomUUID());
                transfer.setCreditAccountId(account1.getId());
                transfer.setDebitAccountId(account2.getId());
                transfer.setLedger(720);
                transfer.setCode((short) 1);
                transfer.setAmount(100);

                Future<CreateTransfersResult[]> createTransfersErrorsFuture =
                        client.createTransfersAsync(new Transfer[] {transfer});
                assertFalse(createTransfersErrorsFuture.isDone());

                var createTransfersErrors = createTransfersErrorsFuture.get();
                assertTrue(createTransfersErrors.length == 0);

                Future<Account[]> lookupAccountsFuture =
                        client.lookupAccountsAsync(new UUID[] {account1.getId(), account2.getId()});
                assertFalse(lookupAccountsFuture.isDone());

                var lookupAccounts = lookupAccountsFuture.get();
                assertAccounts(account1, lookupAccounts[0]);
                assertAccounts(account2, lookupAccounts[1]);

                assertEquals(transfer.getAmount(), lookupAccounts[0].getCreditsPosted());
                assertEquals(0L, lookupAccounts[0].getDebitsPosted());

                assertEquals(transfer.getAmount(), lookupAccounts[1].getDebitsPosted());
                assertEquals(0L, lookupAccounts[1].getCreditsPosted());

                Future<Transfer[]> lookupTransfersFuture =
                        client.lookupTransfersAsync(new UUID[] {transfer.getId()});
                assertFalse(lookupTransfersFuture.isDone());

                var lookupTransfers = lookupTransfersFuture.get();
                assertTrue(lookupTransfers.length == 1);

                assertTransfers(transfer, lookupTransfers[0]);
                assertNotEquals(0L, lookupTransfers[0].getTimestamp());

            } catch (Throwable any) {
                throw any;
            }

        } catch (Throwable any) {
            throw any;
        }
    }

    @Test
    public void testCreateTransfersAsyncBatch() throws Throwable {

        try (var server = new Server()) {
            try (var client = new Client(0, new String[] {Server.TB_PORT})) {

                var accountsBatch = new AccountsBatch(2);
                accountsBatch.add(account1);
                accountsBatch.add(account2);
                Future<CreateAccountsResult[]> createAccountsErrorsFuture =
                        client.createAccountsAsync(accountsBatch);
                assertFalse(createAccountsErrorsFuture.isDone());

                var createAccountsErrors = createAccountsErrorsFuture.get();
                assertTrue(createAccountsErrors.length == 0);

                var transfer = new Transfer();
                transfer.setId(UUID.randomUUID());
                transfer.setCreditAccountId(account1.getId());
                transfer.setDebitAccountId(account2.getId());
                transfer.setLedger(720);
                transfer.setCode((short) 1);
                transfer.setAmount(100);

                var transfersBatch = new TransfersBatch(1);
                transfersBatch.add(transfer);

                Future<CreateTransfersResult[]> createTransferErrorsFuture =
                        client.createTransfersAsync(transfersBatch);
                assertFalse(createTransferErrorsFuture.isDone());

                var createTransferErrors = createTransferErrorsFuture.get();
                assertTrue(createTransferErrors.length == 0);

                var accountsUUIDsBatch = new UUIDsBatch(2);
                accountsUUIDsBatch.add(account1.getId());
                accountsUUIDsBatch.add(account2.getId());

                Future<Account[]> lookupAccountsFuture =
                        client.lookupAccountsAsync(accountsUUIDsBatch);
                assertFalse(lookupAccountsFuture.isDone());

                var lookupAccounts = lookupAccountsFuture.get();
                assertAccounts(account1, lookupAccounts[0]);
                assertAccounts(account2, lookupAccounts[1]);

                assertEquals(transfer.getAmount(), lookupAccounts[0].getCreditsPosted());
                assertEquals(0L, lookupAccounts[0].getDebitsPosted());

                assertEquals(transfer.getAmount(), lookupAccounts[1].getDebitsPosted());
                assertEquals(0L, lookupAccounts[1].getCreditsPosted());

                var transfersUUIDsBatch = new UUIDsBatch(1);
                transfersUUIDsBatch.add(transfer.getId());
                Future<Transfer[]> lookupTransfersFuture =
                        client.lookupTransfersAsync(transfersUUIDsBatch);
                assertFalse(lookupTransfersFuture.isDone());

                var lookupTransfers = lookupTransfersFuture.get();
                assertTrue(lookupTransfers.length == 1);

                assertTransfers(transfer, lookupTransfers[0]);
                assertNotEquals(0L, lookupTransfers[0].getTimestamp());

            } catch (Throwable any) {
                throw any;
            }

        } catch (Throwable any) {
            throw any;
        }
    }

    @Test
    public void testCreateSingleTransfer() throws Throwable {

        try (var server = new Server()) {
            try (var client = new Client(0, new String[] {Server.TB_PORT})) {

                var account1Result = client.createAccount(account1);
                assertTrue(account1Result == CreateAccountResult.Ok);

                var account2Result = client.createAccount(account2);
                assertTrue(account2Result == CreateAccountResult.Ok);

                var transfer = new Transfer();
                transfer.setId(UUID.randomUUID());
                transfer.setCreditAccountId(account1.getId());
                transfer.setDebitAccountId(account2.getId());
                transfer.setLedger(720);
                transfer.setCode((short) 1);
                transfer.setAmount(100);

                var transferResult = client.createTransfer(transfer);
                assertTrue(transferResult == CreateTransferResult.Ok);

                var lookupAccount1 = client.lookupAccount(account1.getId());
                assertAccounts(account1, lookupAccount1);

                var lookupAccount2 = client.lookupAccount(account2.getId());
                assertAccounts(account2, lookupAccount2);

                assertEquals(lookupAccount1.getCreditsPosted(), transfer.getAmount());
                assertEquals(lookupAccount1.getDebitsPosted(), (long) 0);

                assertEquals(lookupAccount2.getDebitsPosted(), transfer.getAmount());
                assertEquals(lookupAccount2.getCreditsPosted(), (long) 0);

                var lookupTransfer = client.lookupTransfer(transfer.getId());
                assertNotNull(lookupTransfer);

                assertTransfers(transfer, lookupTransfer);
                assertNotEquals(0L, lookupTransfer.getTimestamp());

            } catch (Throwable any) {
                throw any;
            }

        } catch (Throwable any) {
            throw any;
        }
    }

    @Test
    public void testCreateInvalidTransfer() throws Throwable {

        try (var server = new Server()) {
            try (var client = new Client(0, new String[] {Server.TB_PORT})) {

                var transfer = new Transfer();
                var transferResult = client.createTransfer(transfer);
                assertTrue(transferResult == CreateTransferResult.IdMustNotBeZero);

                var lookupTransfer = client.lookupTransfer(transfer.getId());
                assertNull(lookupTransfer);

            } catch (Throwable any) {
                throw any;
            }

        } catch (Throwable any) {
            throw any;
        }
    }

    @Test
    public void testCreatePendingTransfers() throws Throwable {

        try (var server = new Server()) {
            try (var client = new Client(0, new String[] {Server.TB_PORT})) {

                var errors = client.createAccounts(new Account[] {account1, account2});
                assertTrue(errors.length == 0);

                var transfer = new Transfer();
                transfer.setId(UUID.randomUUID());
                transfer.setCreditAccountId(account1.getId());
                transfer.setDebitAccountId(account2.getId());
                transfer.setLedger(720);
                transfer.setCode((short) 1);
                transfer.setAmount(100);
                transfer.setFlags(TransferFlags.PENDING);
                transfer.setTimeout(Integer.MAX_VALUE);

                var result = client.createTransfer(transfer);
                assertTrue(result == CreateTransferResult.Ok);

                var lookupAccounts =
                        client.lookupAccounts(new UUID[] {account1.getId(), account2.getId()});
                assertAccounts(account1, lookupAccounts[0]);
                assertAccounts(account2, lookupAccounts[1]);

                assertEquals(lookupAccounts[0].getCreditsPending(), transfer.getAmount());
                assertEquals(lookupAccounts[0].getDebitsPending(), (long) 0);
                assertEquals(lookupAccounts[0].getCreditsPosted(), (long) 0);
                assertEquals(lookupAccounts[0].getDebitsPosted(), (long) 0);

                assertEquals(lookupAccounts[1].getDebitsPending(), transfer.getAmount());
                assertEquals(lookupAccounts[1].getCreditsPending(), (long) 0);
                assertEquals(lookupAccounts[1].getDebitsPosted(), (long) 0);
                assertEquals(lookupAccounts[1].getCreditsPosted(), (long) 0);

                var lookupTransfer = client.lookupTransfer(transfer.getId());
                assertNotNull(lookupTransfer);

                assertTransfers(transfer, lookupTransfer);
                assertNotEquals(0L, lookupTransfer.getTimestamp());

                var postTransfer = new Transfer();
                postTransfer.setId(UUID.randomUUID());
                postTransfer.setCreditAccountId(account1.getId());
                postTransfer.setDebitAccountId(account2.getId());
                postTransfer.setLedger(720);
                postTransfer.setCode((short) 1);
                postTransfer.setAmount(100);
                postTransfer.setFlags(TransferFlags.POST_PENDING_TRANSFER);
                postTransfer.setPendingId(transfer.getId());

                var postResult = client.createTransfer(postTransfer);
                assertTrue(postResult == CreateTransferResult.Ok);

                lookupAccounts =
                        client.lookupAccounts(new UUID[] {account1.getId(), account2.getId()});
                assertAccounts(account1, lookupAccounts[0]);
                assertAccounts(account2, lookupAccounts[1]);

                assertEquals(lookupAccounts[0].getCreditsPosted(), transfer.getAmount());
                assertEquals(lookupAccounts[0].getDebitsPosted(), (long) 0);
                assertEquals(lookupAccounts[0].getCreditsPending(), (long) 0);
                assertEquals(lookupAccounts[0].getDebitsPending(), (long) 0);

                assertEquals(lookupAccounts[1].getDebitsPosted(), transfer.getAmount());
                assertEquals(lookupAccounts[1].getCreditsPosted(), (long) 0);
                assertEquals(lookupAccounts[1].getDebitsPending(), (long) 0);
                assertEquals(lookupAccounts[1].getCreditsPending(), (long) 0);

                var lookupPostTransfer = client.lookupTransfer(postTransfer.getId());
                assertNotNull(lookupPostTransfer);

                assertTransfers(postTransfer, lookupPostTransfer);
                assertNotEquals(0L, lookupPostTransfer.getTimestamp());

            } catch (Throwable any) {
                throw any;
            }

        } catch (Throwable any) {
            throw any;
        }
    }

    @Test
    public void testCreatePendingTransfersAndVoid() throws Throwable {

        try (var server = new Server()) {
            try (var client = new Client(0, new String[] {Server.TB_PORT})) {

                var errors = client.createAccounts(new Account[] {account1, account2});
                assertTrue(errors.length == 0);

                var transfer = new Transfer();
                transfer.setId(UUID.randomUUID());
                transfer.setCreditAccountId(account1.getId());
                transfer.setDebitAccountId(account2.getId());
                transfer.setLedger(720);
                transfer.setCode((short) 1);
                transfer.setAmount(100);
                transfer.setFlags(TransferFlags.PENDING);
                transfer.setTimeout(Integer.MAX_VALUE);

                var result = client.createTransfer(transfer);
                assertTrue(result == CreateTransferResult.Ok);

                var lookupAccounts =
                        client.lookupAccounts(new UUID[] {account1.getId(), account2.getId()});
                assertAccounts(account1, lookupAccounts[0]);
                assertAccounts(account2, lookupAccounts[1]);

                assertEquals(lookupAccounts[0].getCreditsPending(), transfer.getAmount());
                assertEquals(lookupAccounts[0].getDebitsPending(), (long) 0);
                assertEquals(lookupAccounts[0].getCreditsPosted(), (long) 0);
                assertEquals(lookupAccounts[0].getDebitsPosted(), (long) 0);

                assertEquals(lookupAccounts[1].getDebitsPending(), transfer.getAmount());
                assertEquals(lookupAccounts[1].getCreditsPending(), (long) 0);
                assertEquals(lookupAccounts[1].getDebitsPosted(), (long) 0);
                assertEquals(lookupAccounts[1].getCreditsPosted(), (long) 0);

                var lookupTransfer = client.lookupTransfer(transfer.getId());
                assertNotNull(lookupTransfer);

                assertTransfers(transfer, lookupTransfer);
                assertNotEquals(0L, lookupTransfer.getTimestamp());

                var voidTransfer = new Transfer();
                voidTransfer.setId(UUID.randomUUID());
                voidTransfer.setCreditAccountId(account1.getId());
                voidTransfer.setDebitAccountId(account2.getId());
                voidTransfer.setLedger(720);
                voidTransfer.setCode((short) 1);
                voidTransfer.setAmount(100);
                voidTransfer.setFlags(TransferFlags.VOID_PENDING_TRANSFER);
                voidTransfer.setPendingId(transfer.getId());

                var postResult = client.createTransfer(voidTransfer);
                assertTrue(postResult == CreateTransferResult.Ok);

                lookupAccounts =
                        client.lookupAccounts(new UUID[] {account1.getId(), account2.getId()});
                assertAccounts(account1, lookupAccounts[0]);
                assertAccounts(account2, lookupAccounts[1]);

                assertEquals(lookupAccounts[0].getCreditsPosted(), (long) 0);
                assertEquals(lookupAccounts[0].getDebitsPosted(), (long) 0);
                assertEquals(lookupAccounts[0].getCreditsPending(), (long) 0);
                assertEquals(lookupAccounts[0].getDebitsPending(), (long) 0);

                assertEquals(lookupAccounts[1].getDebitsPosted(), (long) 0);
                assertEquals(lookupAccounts[1].getCreditsPosted(), (long) 0);
                assertEquals(lookupAccounts[1].getDebitsPending(), (long) 0);
                assertEquals(lookupAccounts[1].getCreditsPending(), (long) 0);

                var lookupVoidTransfer = client.lookupTransfer(voidTransfer.getId());
                assertNotNull(lookupVoidTransfer);

                assertTransfers(voidTransfer, lookupVoidTransfer);
                assertNotEquals(0L, lookupVoidTransfer.getTimestamp());

            } catch (Throwable any) {
                throw any;
            }

        } catch (Throwable any) {
            throw any;
        }
    }

    @Test
    public void testCreateLinkedTransfers() throws Throwable {

        try (var server = new Server()) {
            try (var client = new Client(0, new String[] {Server.TB_PORT})) {

                var errors = client.createAccounts(new Account[] {account1, account2});
                assertTrue(errors.length == 0);

                var transfer1 = new Transfer();
                transfer1.setId(UUID.randomUUID());
                transfer1.setCreditAccountId(account1.getId());
                transfer1.setDebitAccountId(account2.getId());
                transfer1.setLedger(720);
                transfer1.setCode((short) 1);
                transfer1.setAmount(100);
                transfer1.setFlags(TransferFlags.LINKED);

                var transfer2 = new Transfer();
                transfer2.setId(UUID.randomUUID());
                transfer2.setCreditAccountId(account2.getId());
                transfer2.setDebitAccountId(account1.getId());
                transfer2.setLedger(720);
                transfer2.setCode((short) 1);
                transfer2.setAmount(49);
                transfer2.setFlags(TransferFlags.NONE);

                var transfersErrors = client.createTransfers(new Transfer[] {transfer1, transfer2});
                assertTrue(transfersErrors.length == 0);

                var lookupAccounts =
                        client.lookupAccounts(new UUID[] {account1.getId(), account2.getId()});
                assertAccounts(account1, lookupAccounts[0]);
                assertAccounts(account2, lookupAccounts[1]);

                assertEquals(lookupAccounts[0].getCreditsPosted(), transfer1.getAmount());
                assertEquals(lookupAccounts[0].getDebitsPosted(), transfer2.getAmount());
                assertEquals(lookupAccounts[0].getCreditsPending(), (long) 0);
                assertEquals(lookupAccounts[0].getDebitsPending(), (long) 0);

                assertEquals(lookupAccounts[1].getCreditsPosted(), transfer2.getAmount());
                assertEquals(lookupAccounts[1].getDebitsPosted(), transfer1.getAmount());
                assertEquals(lookupAccounts[1].getCreditsPending(), (long) 0);
                assertEquals(lookupAccounts[1].getDebitsPending(), (long) 0);

                var lookupTransfers =
                        client.lookupTransfers(new UUID[] {transfer1.getId(), transfer2.getId()});
                assertEquals(2, lookupTransfers.length);

                assertTransfers(transfer1, lookupTransfers[0]);
                assertTransfers(transfer2, lookupTransfers[1]);
                assertNotEquals(0L, lookupTransfers[0].getTimestamp());
                assertNotEquals(0L, lookupTransfers[1].getTimestamp());

            } catch (Throwable any) {
                throw any;
            }

        } catch (Throwable any) {
            throw any;
        }
    }

    @Test
    public void testCreateAccountTooMuchData() throws Throwable {

        try (var server = new Server()) {
            try (var client = new Client(0, new String[] {Server.TB_PORT})) {

                final int TOO_MUCH_DATA = 10000;
                var batch = new AccountsBatch(TOO_MUCH_DATA);
                for (int i = 0; i < TOO_MUCH_DATA; i++) {
                    var account = new Account();
                    account.setId(UUID.randomUUID());
                    account.setCode(1);
                    account.setLedger(1);
                    batch.add(account);
                }

                try {
                    client.createAccounts(batch);
                    assert false;
                } catch (RequestException requestException) {

                    assertEquals(RequestException.Status.TOO_MUCH_DATA,
                            requestException.getStatus());

                }

            } catch (Throwable any) {
                throw any;
            }

        } catch (Throwable any) {
            throw any;
        }
    }

    @Test
    public void testCreateTransferTooMuchData() throws Throwable {

        try (var server = new Server()) {

            try (var client = new Client(0, new String[] {Server.TB_PORT})) {

                final int TOO_MUCH_DATA = 10000;
                var batch = new TransfersBatch(TOO_MUCH_DATA);

                for (int i = 0; i < TOO_MUCH_DATA; i++) {
                    var transfer = new Transfer();
                    transfer.setId(UUID.randomUUID());
                    transfer.setDebitAccountId(account1.getId());
                    transfer.setDebitAccountId(account2.getId());
                    transfer.setCode(1);
                    transfer.setLedger(1);
                    batch.add(transfer);
                }

                try {
                    client.createTransfers(batch);
                    assert false;
                } catch (RequestException requestException) {

                    assertEquals(RequestException.Status.TOO_MUCH_DATA,
                            requestException.getStatus());

                }

            } catch (Throwable any) {
                throw any;
            }

        } catch (Throwable any) {
            throw any;
        }
    }


    /**
     * This test asserts that parallel threads will respect client's maxConcurrency.
     *
     * @throws Throwable
     */
    @Test
    public void testConcurrentTasks() throws Throwable {

        try (var server = new Server()) {

            // Defining a ratio between concurrent threads and client's maxConcurrency
            // The goal here is to force to have more threads than the client can process
            // simultaneously
            final int tasks_qty = 12;
            final int max_concurrency = tasks_qty / 4;

            try (var client = new Client(0, new String[] {Server.TB_PORT}, max_concurrency)) {

                var errors = client.createAccounts(new Account[] {account1, account2});
                assertTrue(errors.length == 0);

                var tasks = new TransferTask[tasks_qty];
                for (int i = 0; i < tasks_qty; i++) {
                    // Starting multiple threads submiting transfers,
                    tasks[i] = new TransferTask(client);
                    tasks[i].start();
                }

                // Wait for all threads
                for (int i = 0; i < tasks_qty; i++) {
                    tasks[i].join();
                    assertFalse(tasks[i].isFaulted);
                    assertEquals(tasks[i].result, CreateTransferResult.Ok);
                }

                // Asserting if all transfers were submited correctly
                var lookupAccounts =
                        client.lookupAccounts(new UUID[] {account1.getId(), account2.getId()});
                assertAccounts(account1, lookupAccounts[0]);
                assertAccounts(account2, lookupAccounts[1]);

                assertEquals(lookupAccounts[0].getCreditsPosted(), (long) (100 * tasks_qty));
                assertEquals(lookupAccounts[0].getDebitsPosted(), (long) 0);

                assertEquals(lookupAccounts[1].getDebitsPosted(), (long) (100 * tasks_qty));
                assertEquals(lookupAccounts[1].getCreditsPosted(), (long) 0);

            } catch (Throwable any) {
                throw any;
            }

        } catch (Throwable any) {
            throw any;
        }
    }

    /**
     * This test asserts that client.close() will wait for all ongoing request to complete And new
     * threads trying to submit a request after the client was closed will fail with
     * IllegalStateException.
     *
     * @throws Throwable
     */
    @Test
    public void testCloseWithConcurrentTasks() throws Throwable {

        try (var server = new Server()) {

            // Defining a ratio between concurrent threads and client's maxConcurrency
            // The goal here is to force to have way more threads than the client can
            // process simultaneously
            final int tasks_qty = 12;
            final int max_concurrency = tasks_qty / 4;

            try (var client = new Client(0, new String[] {Server.TB_PORT}, max_concurrency)) {

                var errors = client.createAccounts(new Account[] {account1, account2});
                assertTrue(errors.length == 0);

                var tasks = new TransferTask[tasks_qty];
                for (int i = 0; i < tasks_qty; i++) {

                    // Starting multiple threads submiting transfers,
                    tasks[i] = new TransferTask(client);
                    tasks[i].start();
                }

                // Wait just for one thread to complete
                tasks[0].join();

                // And them close the client while several threads are still working
                // Some of them have already submited the request, others are waiting due to the
                // maxConcurrency limit
                client.close();

                for (int i = 0; i < tasks_qty; i++) {

                    // The client.close must wait until all submited requests have completed
                    // Asserting that either the task succeeded or failed while waiting
                    tasks[i].join();
                    assertTrue(tasks[i].isFaulted || tasks[i].result == CreateTransferResult.Ok);
                }

            } catch (Throwable any) {
                throw any;
            }

        } catch (Throwable any) {
            throw any;
        }
    }

    private static void assertAccounts(Account account1, Account account2) {
        assertEquals(account1.getId(), account2.getId());
        assertEquals(account1.getUserData(), account2.getUserData());
        assertEquals(account1.getLedger(), account2.getLedger());
        assertEquals(account1.getCode(), account2.getCode());
        assertEquals(account1.getFlags(), account2.getFlags());
    }

    private static void assertTransfers(Transfer transfer1, Transfer transfer2) {
        assertEquals(transfer1.getId(), transfer2.getId());
        assertEquals(transfer1.getCreditAccountId(), transfer2.getCreditAccountId());
        assertEquals(transfer1.getDebitAccountId(), transfer2.getDebitAccountId());
        assertEquals(transfer1.getUserData(), transfer2.getUserData());
        assertEquals(transfer1.getLedger(), transfer2.getLedger());
        assertEquals(transfer1.getCode(), transfer2.getCode());
        assertEquals(transfer1.getFlags(), transfer2.getFlags());
        assertEquals(transfer1.getAmount(), transfer2.getAmount());
        assertEquals(transfer1.getTimeout(), transfer2.getTimeout());
        assertEquals(transfer1.getPendingId(), transfer2.getPendingId());
    }

    private class TransferTask extends Thread {

        public final Client client;
        public CreateTransferResult result;
        public boolean isFaulted;

        public TransferTask(Client client) {
            this.client = client;
            this.result = CreateTransferResult.Ok;
            this.isFaulted = false;
        }

        @Override
        public synchronized void run() {
            var transfer = new Transfer();
            transfer.setId(UUID.randomUUID());
            transfer.setCreditAccountId(account1.getId());
            transfer.setDebitAccountId(account2.getId());
            transfer.setLedger(720);
            transfer.setCode((short) 1);
            transfer.setAmount(100);

            try {
                result = client.createTransfer(transfer);
            } catch (Throwable e) {
                isFaulted = true;
            }
        }
    }

    private class Server implements AutoCloseable {

        public static final String TB_EXE = "tigerbeetle";
        public static final String TB_PORT = "3001";
        public static final String TB_FILE = "./java-tests.tigerbeetle";
        public static final String TB_PATH = "../zig/lib/tigerbeetle/";
        public static final String TB_SERVER = TB_PATH + "/" + TB_EXE;

        private Process process;

        public Server() throws IOException, OperationsException, InterruptedException {

            cleanUp();

            var format = Runtime.getRuntime().exec(
                    new String[] {TB_SERVER, "format", "--cluster=0", "--replica=0", TB_FILE});
            if (format.waitFor() != 0) {
                var reader = new BufferedReader(new InputStreamReader(format.getErrorStream()));
                var error = reader.lines().collect(Collectors.joining(". "));
                throw new OperationsException("Format failed. " + error);
            }

            this.process = Runtime.getRuntime()
                    .exec(new String[] {TB_SERVER, "start", "--addresses=" + TB_PORT, TB_FILE});
            if (process.waitFor(100, TimeUnit.MILLISECONDS))
                throw new OperationsException("Start server failed");
        }

        @Override
        public void close() throws Exception {
            cleanUp();
        }

        private void cleanUp() throws OperationsException {
            try {

                if (process != null && process.isAlive()) {
                    process.destroy();
                }

                var file = new File("./" + TB_FILE);
                file.delete();
            } catch (Throwable any) {
                throw new OperationsException("Cleanup has failed");
            }
        }
    }

}
