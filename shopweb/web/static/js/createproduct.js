(function() {
	$(function() {

		$("#create_product").click(function(event) {
			window.admin.save("#upload_form", function() {
			});
			return false;
		});

	});

})();
