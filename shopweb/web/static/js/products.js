(function() {
	$(function() {
		$(".add_to_cart_box").click(function(event) {
			var pid = $(this).attr("id");
			cart.addItem(pid);
			window.common.showNotice("Produsul a fost adăugat in coș.")
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

		$("figure").each(function(i) {
			$(this).click(function(e) {
				window.location.href = "/product/" + $(this).attr("id");
				e.stopPropagation();
				return false;
			})
		})
	});
})();