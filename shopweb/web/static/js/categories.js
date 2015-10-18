(function() {
    $(function() {

        $.blockUI.defaults.baseZ = 90;
        categories.reloadCategories();

        var total = $("#presentation ul li").length;

        var buf = $("#presentation ul li").clone();

        var presentationFunc = function() {
            $("#presentation ul").animate({
                "marginLeft" : "-=214px"
            }, 4000, "linear", function() {
                console.log("done");
                first = buf[0];
                buf.splice(0, 1);
                buf.push(first);
                console.log(buf);
                $("#presentation ul").replaceWith($("<ul></ul>").append(buf));
                buf = buf.clone();
                window.setTimeout(presentationFunc, 0);
            });

        }

        window.setTimeout(presentationFunc, 0);
    });
})();

var categories = {

    refreshList : function() {

        $(".close_dialog").bind("click", function(event) {
            window.common.closeDialog();
            return false;
        });

        $(".cat_box").each(function(index) {
            var me = $(this);
            var pid = me.attr("id");

            if (window.admin !== undefined) {
                me.find('.edit_tag_close').click(function(event) {
                    window.admin.deleteCategory(pid);
                    return false;
                });

                me.find('.edit_tag_update').click(function(event) {
                    window.admin.editCategory(pid);
                    return false;
                });

            }

            if (pid !== undefined) {
                me.click(function(event) {
                    window.location.href = "/products?cat=" + pid;
                    return false;
                });
            } else {
                if (window.admin !== undefined) {
                    window.admin.attachCreateCategory(me);
                }
            }
        });
    },

    reloadCategories : function() {
        $.ajax({
            url : "/",
            cache : false,
            dataType : "html",
            error : function(xhr) {
                common.showError(xhr.statusText);
            },
            success : function(data) {
                $("#item_list").html(data);

                categories.refreshList();
                $("#item_list").waitForImages(function() {
                    $("#item_list").fadeIn({
                        duration : 1000
                    });
                });

                return false;
            }
        });

    }
}