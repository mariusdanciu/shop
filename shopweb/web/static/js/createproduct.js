(function() {
	$(function() {

		$("#create_product").click(function(event) {
			window.admin.save("#upload_form", function(data) {
				window.location.href = "/product?pid=" + data.pid;
			});
			return false;
		});

	});

})();
