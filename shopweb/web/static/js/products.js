(function() {
	$(function() {
		$(".add_to_cart_box").click(function(event) {
			var pid = $(this).attr("id");
			cart.addItem(pid);
			window.common.showNotice("Produsul a fost adăugat in coș.", $(this).parent());
			event.stopPropagation();
			return false;
		});

		$("#sortSelect").change(function(event) {

			var sel = $("#sortSelect option:selected").val();

			var cat = $.url().segment(2);
			var search = $.url().param("search")

			var url = "/products/" + cat + "?sort=" + sel;

			if (search !== undefined) {
				url += "&search=" + search;
			}

			window.location.href = url

			event.stopPropagation();
			return false;
		});

		$(".delete_category").click(function(event) {
        	var id = $(this).attr("data_cat");
        	window.admin.deleteCategory(id, function() {
        	  window.location.href = "/"
        	});
        	event.stopPropagation();
        	return false;
        });

	});
})();