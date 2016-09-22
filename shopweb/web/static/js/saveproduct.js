(function() {
	$(function() {


		$("#create_product").click(function(event) {
			window.common.save("#upload_form", function(data) {
				window.location.href = "/product/" + data.pid;
			});
			return false;
		});

	});

})();
