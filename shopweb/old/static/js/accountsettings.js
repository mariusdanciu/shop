(function() {
    $(function() {

        $('#addr_name').keyup(function(e) {
            if (e.keyCode == 13) {
                settings.addAddress();
            }
        });

        $('#update_user').click(function(e) {
            settings.updateUser("#updatesettings_form");
            return false;
        });

        $('#delete_user').click(function(e) {
            settings.deleteUser();
            return false;
        });

        $("#addresses").find('.del').click(function(event) {
            var t = $(this).parent();
            var c = t.next();
            t.remove();
            c.remove();
            return false;
        });

        $("#my_orders").click(function(event) {
            settings.myOrders("#my_orders_result");
            return false;
        });

        $("#settings_tab").tabify();
        settings.refreshAccordion();

    })
})();

var opened = undefined;

var settings = {
    myOrders : function(selector) {
        $(selector).load("/ordersview/myorders", function(response, status, xhr) {
            if (status === "error") {
                common.showError(xhr.statusText);
            } else {
                settings.refreshAccordion();
            }
        });
    },

    deleteUser : function() {
        $.ajax({
            url : "delete/user",
            type : "DELETE",
            cache : false,
            timeout : 3000,
            statusCode : {
                200 : function(msg) {
                    window.location.href = "/";
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

    updateUser : function(formId) {
        $(formId).each(function() {
            var frm = this;

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
        });
    },

    addAddress : function() {
        var name = $('#addr_name').val();

        if (name.trim()) {
            var title = $('.address_template .accordion_title').clone();
            var content = $('.address_template .accordion_content').clone();

            title.find('.addr_title').text(name);
            content.find("label").each(function(e) {
                $(this).attr("for", $(this).attr("for") + name);
            });
            content.find("input").each(function(e) {
                $(this).attr("name", $(this).attr("name") + name);
                $(this).attr("id", $(this).attr("id") + name);
            });

            $('#addresses').append(title).append(content);

            title.find('.del').click(function(event) {
                var t = $(this).parent();
                var c = t.next();
                t.remove();
                c.remove();
                return false;
            });

            $('.accordion > .accordion_title').unbind();
            allPanels = $('.accordion > .accordion_content');
            settings.refreshAccordion();
        }
    },

    refreshAccordion : function() {
        $('.accordion > .accordion_title').unbind();
        $('.order_edit_status').unbind();
        opened = undefined;
        $('.accordion > .accordion_title').click(function() {
            var allPanels = $('.accordion > .accordion_content');
            allPanels.slideUp();
            if (opened !== this) {
                $(this).next().slideDown();
                opened = this;
            } else {
                opened = undefined;
            }
            return false;
        });
        $('.order_edit_status').click(function(e) {
            return false;
        });
        $('.order_edit_status').change(function(e) {
            var self = $(this);
            $.ajax({
                url : '/order/updatestatus/' + self.attr("id") + "/" + self.val(),
                type : "POST",
                cache : false,
                timeout : 3000,
            });

        });

    }

}