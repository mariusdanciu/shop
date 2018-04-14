(function() {
	$(function() {

		$("#save_user").click(function(event) {
			window.settings.updateUser("#updatesettings_form")
			event.preventDefault();
			return false;
		});

		

		$("#delete_user").click(function(event) {
			window.settings.deleteUser()
			event.preventDefault();
			return false;
		});
	});

})();

