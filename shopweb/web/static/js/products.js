(function() {
	$(function() {
		console.log("laoded");
		$(".add_to_cart_box").click(function(event) {
			var pid = $(this).attr("id");
			console.log(pid);
			cart.addItem(pid);
			event.stopPropagation();
			return false;
		});
	});
})();