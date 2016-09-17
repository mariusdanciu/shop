(function() {
	$(function() {
		$(".add_to_cart_box").click(function(event) {
			var pid = $(this).attr("id");
			cart.addItem(pid);
			event.stopPropagation();
			return false;
		});

		$("#sortSelect").change(function(event) {

			var sel = $("#sortSelect option:selected").val();
			
			var cat = $.url().param("cat");

			window.location.href = "/products?cat=" + cat + "&sort=" + sel;
			event.stopPropagation();
			return false;
		});
	});
})();