package org.gbif.registry.guice;

import org.gbif.api.model.common.User;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.common.IdentityService;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.identity.model.Session;
import org.gbif.identity.model.UserModelMutationResult;
import org.gbif.registry.TestConstants;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;

/**
 * Class that implements an in-memory users store.
 * This class is intended to be use in IT and unit tests ONLY and only provides methods to authenticate.
 */
public class IdentityServiceMock implements IdentityService {

  private static final Map<String, User> USERS = Maps.newHashMap();
  static{
    USERS.put(TestConstants.TEST_ADMIN, mockUser(1, TestConstants.TEST_ADMIN,
            TestConstants.getTestUserRole(TestConstants.TEST_ADMIN)));
    USERS.put(TestConstants.TEST_EDITOR, mockUser(2, TestConstants.TEST_EDITOR,
            TestConstants.getTestUserRole(TestConstants.TEST_EDITOR)));
    USERS.put(TestConstants.TEST_USER, mockUser(3, TestConstants.TEST_USER,
            TestConstants.getTestUserRole(TestConstants.TEST_USER)));
  }

  private static final String EMAIL_AT_GIBF = "%s@gbif.test.org";

  @Nullable
  @Override
  public User getByKey(int id) {
    return USERS.entrySet().stream()
            .map(Map.Entry::getValue)
            .filter(user -> user.getKey().equals(id))
            .findFirst()
            .map(IdentityServiceMock::copyUserAfterLogin)
            .orElse(null);
  }

  /**
   * Authenticates the user by a simple match of username and pwd.
   */
  @Override
  public User authenticate(String username, String passwordHash) {
    if (USERS.containsKey(username) && USERS.get(username).getPasswordHash().equals(passwordHash)) {
      return copyUserAfterLogin(USERS.get(username));
    }
    return null;
  }

  @Override
  public void delete(int key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public UserModelMutationResult update(User user) {
    throw new UnsupportedOperationException();
  }

  @Override
  public User get(String username) {
    if (USERS.containsKey(username)) {
      return copyUserAfterLogin(USERS.get(username));
    }
    return null;
  }

  @Nullable
  @Override
  public User getByEmail(String email) {
    return USERS.entrySet().stream()
            .map(Map.Entry::getValue)
            .filter(user -> user.getEmail().equals(email))
            .findFirst()
            .map(IdentityServiceMock::copyUserAfterLogin)
            .orElse(null);
  }

  @Nullable
  @Override
  public User getByIdentifier(String identifier) {
    return StringUtils.contains(identifier, "@") ?
            getByEmail(identifier) :get(identifier);
  }

  @Override
  public PagingResponse<User> list(@Nullable Pageable pageable) {
    throw new UnsupportedOperationException();
  }

  /**
   * The session key must be equals to the user name.
   */
  @Override
  public User getBySession(String session) {
    return mockUser(1, session, null);
  }

  @Override
  public PagingResponse<User> search(String query, @Nullable Pageable page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public UserModelMutationResult create(User user, String password) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Session createSession(String username) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void terminateSession(String session) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void terminateAllSessions(String username) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void updateLastLogin(int userKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isChallengeCodeValid(int userKey, UUID challengeCode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean confirmChallengeCode(int userKey, UUID challengeCode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsChallengeCode(int userKey) {
    return false;
  }

  @Override
  public boolean updatePassword(int userKey, String newPassword, UUID challengeCode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void resetPassword(int userKey) {
    throw new UnsupportedOperationException();
  }

  /**
   * Return a copy of the user with the lastLogin date set to now.
   * @param user
   * @return
   */
  private static User copyUserAfterLogin(User user) {
    User userCopy = new User();
    userCopy.setUserName(user.getUserName());
    userCopy.setPasswordHash(user.getPasswordHash());
    userCopy.setLastLogin(new Date());
    userCopy.setEmail(user.getEmail());
    userCopy.setFirstName(user.getFirstName());
    userCopy.setLastName(user.getLastName());
    userCopy.setKey(user.getKey());
    if (user.getRoles()!=null) {
      userCopy.setRoles(Sets.newHashSet(user.getRoles()));
    }
    return userCopy;
  }

  /**
   * Creates a full user instance using the username as passwordhash
   */
  private static User mockUser(int key, String username, UserRole... roles) {
    User user = new User();
    user.setUserName(username);
    user.setPasswordHash(username);
    user.setLastLogin(new Date());
    user.setEmail(String.format(EMAIL_AT_GIBF, username));
    user.setFirstName(username);
    user.setLastName(username);
    user.setKey(key);
    if (roles!=null) {
      user.setRoles(Sets.newHashSet(roles));
    }
    return user;
  }

}