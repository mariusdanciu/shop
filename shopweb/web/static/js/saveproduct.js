(function() {
	$(function() {


		$("#save_product").click(function(event) {
			window.common.save("#upload_form", function(data) {
				window.location.href = data.href;
			});
			return false;
		});

	});

})();
