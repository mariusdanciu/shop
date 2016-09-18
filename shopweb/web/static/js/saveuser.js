(function() {
	$(function() {

		$("#save_user").click(function(event) {
			window.user.createUser("#newuser_form");
			event.preventDefault();
			return false;
		});

	});

})();
