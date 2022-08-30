const Bomb = function (url) {
    return {
        ping: async function () {
            return fetch(`${url}`);
        },
        Management: function (username, password) {
            let authHeader = "Basic " + btoa(`${username}:${password}`);

            return {
                me: async function () {
                    return fetch(`${url}/management`, {
                        headers: { "Authorization": authHeader }
                    });
                }
            }
        }
    };
};
