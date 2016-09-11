(function() {
	$(function() {
		$(".add_to_cart_box").click(function(event) {
			var pid = $(this).attr("id");
			cart.addItem(pid);
			event.stopPropagation();
			return false;
		});
	});
})();