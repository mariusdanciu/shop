(function() {
    $(function() {
    	$("#cart_num").text(cart.numItems());
    });
})();

var cart = {
	fetchUserInfo : function() {
		$.ajax({
			url : "/userinfo",
			dataType : "json",
			timeout : 3000,
			cache : false,
			context : $("#cart_content")
		}).done(function(data) {
			cart.populateForm(data);
		});
	},

	clear : function() {
		$.cookie("cart", JSON.stringify({
			items : []
		}));
		window.cart.loadView();
	},

	computeTotal : function(add) {
		var total = 0;

		$(".cart_item").each(function() {
			var num = $(this).find("input").val();
			var price = $(this).find(".cart_price").text();
			total += num * price;
		});
		if (add) {
			total += add;
		}

		return parseFloat(total).toFixed(2);
	},

	items : function() {
		var c = $.cookie("cart");
		if (c) {
			return $.parseJSON(c).items;
		}
		return [];
	},
	
	numItems : function() {
		var num = 0;
		var it = this.items();
		for (i in it) {
			num = num + it[i].count;
		}
		return num;
	},

	addItem : function(id) {
		var c = $.cookie("cart");
		if (c) {
			var cart = $.parseJSON(c);
			var a = cart.items;
			var found = undefined;
			for (i in a) {
				if (a[i].id === id) {
					found = a[i];
				}
			}

			if (!found) {
				cart.items.push({
					id : id,
					count : 1
				});
			} else {
				found.count = found.count + 1;
			}
			$.cookie("cart", JSON.stringify(cart));
		} else {
			$.cookie("cart", JSON.stringify({
				items : [ {
					id : id,
					count : 1
				} ]
			}));
		}
		$("#cart_num").text(this.numItems());
	},

	setItemCount : function(pos, count) {
		var c = $.cookie("cart");
		if (c) {
			if (count === "")
				count = 1;

			var cart = $.parseJSON(c);
			var a = cart.items;
			for (i in a) {
				if (i == pos) {
					a[i].count = parseInt(count);
				}
			}
			$.cookie("cart", JSON.stringify(cart));
			$("#cart_num").text(this.numItems());
		}
	},

	removeItem : function(pos) {
		var c = $.cookie("cart");
		if (c) {
			var cart = $.parseJSON(c);
			var a = cart.items;
			var na = [];
			for (i in a) {
				if (i != pos) {
					na.push(a[i])
				}
			}
			cart.items = na;
			$.cookie("cart", JSON.stringify(cart));
			$("#cart_num").text(this.numItems());
		}
	}
	

}