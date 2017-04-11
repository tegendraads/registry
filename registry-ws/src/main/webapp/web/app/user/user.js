angular.module('user', [
  'restangular',
  'services.notifications'])

/**
 * Nested stated provider using dot notation (item.detail has a parent of item) and the
 * nested view is rendered into the parent template ui.view div.  A single controller
 * governs the actions on the page.
 */
.config(['$stateProvider', function ($stateProvider, $stateParams) {
  $stateProvider
      .state('user-search', {
    abstract: true,
    url: '/user-search',
    templateUrl: 'app/user/user-search.tpl.html',
    controller: 'UserSearchCtrl'
  })
  .state('user-search.search', {
    url: '',
    templateUrl: 'app/user/user-results.tpl.html'
  })
  .state('user', {
    url: '/user/{key}',
    abstract: true,
    templateUrl: 'app/user/user-main.tpl.html',
    controller: 'UserCtrl'
  })
  .state('user.detail', {
    url: '',
    templateUrl: 'app/user/user-overview.tpl.html'
  })
  .state('user.edit', {
    url: '',
    templateUrl: 'app/user/user-edit.tpl.html'
  })
}])

.controller('UserSearchCtrl', function ($scope, $state, Restangular, DEFAULT_PAGE_SIZE) {
  var user = Restangular.all("user/search");
  $scope.search = function(q) {
    user.getList({q:q, limit:DEFAULT_PAGE_SIZE}).then(function(data) {
      $scope.resultsCount = data.count;
      $scope.results = data.results;
      $scope.searchString = q;
    });
  }
  $scope.search(""); // start with empty search

  $scope.openUser = function(user) {
    $state.transitionTo('user.detail', {key: user.key})
  }
})

/**
 * The single detail controller
 */
.controller('UserCtrl', function ($scope, $state, $stateParams, notifications, Restangular, DEFAULT_PAGE_SIZE) {
  var key = $stateParams.key;

  var load = function () {
    Restangular.one('user', key).get()
        .then(function (user) {
          $scope.user = user;
          return user;
        })
        .then(function (user) {
          Restangular.all("user/roles").getList().then(function (data) {
            $scope.roles = data;
            $scope.user_roles = {};
            _.each($scope.roles, function (element, index, list) {
              $scope.user_roles[element]= _.contains(user.roles, element)
            });
          })
        })
  }
  load();

  Restangular.all("enumeration/basic/Country").getList().then(function(data){
    $scope.countries = data;
  });
  
  // transitions to a new view, correctly setting up the path
  $scope.transitionTo = function (target) {
    $state.transitionTo('user.' + target, { key: key, type: "user" });
  }

  $scope.save = function (user, user_roles) {
    //clear roles
    user.roles = [];
    _.each(_.keys(user_roles), function(el) {
      if(user_roles[el] === true) {
        user.roles.push(el);
      }
    });

    user.put().then(
      function() {
        notifications.pushForNextRoute("User successfully updated", 'info');
        $scope.transitionTo("detail");
      },
      function(response) {
        notifications.pushForCurrentRoute(response.data, 'error');
      }
    );
  }

  $scope.delete = function (entity) {
    entity.remove().then(
      function() {
        notifications.pushForNextRoute("User successfully deleted", 'info');
        load();
        $scope.transitionTo("detail");
      }
    );
  }

  $scope.restore = function (entity) {
    entity.deleted = undefined;
    entity.put().then(
      function() {
        notifications.pushForCurrentRoute("User successfully restored", 'info');
      },
      function(response) {
        notifications.pushForCurrentRoute(response.data, 'error');
      }
    );
  }


  $scope.cancelEdit = function () {
    load();
    $scope.transitionTo("detail");
  }
});
