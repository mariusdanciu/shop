(function() {
	$(function() {

		$("#save_user").click(function(event) {
			window.settings.updateUser("#updatesettings_form")
			event.preventDefault();
			return false;
		});

		

		$("#delete_user").click(function(event) {
			window.settings.deleteUser()
			event.preventDefault();
			return false;
		});
	});

})();

var settings = {
	myOrders : function(selector) {
		$(selector).load("/ordersview/myorders",
				function(response, status, xhr) {
					if (status === "error") {
						common.showNotice(xhr.statusText);
					} else {
						settings.refreshAccordion();
					}
				});
	},

    deleteUser : function() {
        $.ajax({
            url : "/user",
            type : "DELETE",
            cache : false,
            timeout : 3000,
            statusCode : {
                200 : function(msg) {
                    window.location.href = "/";
                },

                403 : function(msg) {
                    common.showNotice(msg);
                }
            }
        });
    },
	
	updateUser : function(formId) {

		$(formId + ' label').css("color", "#555555").removeAttr("title");
		$.ajax({
			url : $(formId).attr('action'),
			type : "POST",
			cache : false,
			timeout : 3000,
			data : $(formId).serialize(),
			statusCode : {
				201 : function(msg) {
					common.showNotice(msg);
				},

				403 : function(msg) {
					var data = JSON.parse(msg.responseText);
					if (data.errors) {
						common.showFormErrors(data.errors);
					}
				}
			}
		});
	},

}
