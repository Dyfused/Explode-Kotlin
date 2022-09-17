const Bomb = function (url) {
	return {
		ping: async function () {
			return fetch(`${url}`);
		},
		Management: function (username, password) {
			let authHeader = "Basic " + btoa(`${username}:${password}`);

			return {
				/**
				 * 获取当前登录用户信息
				 * @returns 请求
				 */
				me: async function () {
					return fetch(`${url}/management`, {
						headers: { Authorization: authHeader },
					});
				},
				/**
				 * 获取用户信息
				 * @param {string} userId 用户ID
				 * @returns 请求
				 */
				getUser: async function (userId) {
					return fetch(`${url}/management/user/${userId}`, {
						headers: { Authorization: authHeader },
					});
				},
				/**
				 * 注册新用户
				 * @param {string} username 用户名称
				 * @param {string} password 用户密码
				 * @returns 请求
				 */
				registerUser: async function (username, password) {
					return fetch(`${url}/management/user/register`, {
						method: "post",
						headers: { Authorization: authHeader },
						body: JSON.stringify({
							username: username,
							password: password,
						}),
					});
				},
				/**
				 * 获取曲目信息
				 * @param {string} setId 曲目ID
				 * @returns 请求
				 */
				getSet: async function (setId) {
					return fetch(`${url}/management/set/${setId}`, {
						headers: { Authorization: authHeader },
					});
				},
				/**
				 * 获取曲目信息
				 * @param {string} chartId 曲目ID
				 * @returns 请求
				 */
				getChart: async function (chartId) {
					return fetch(`${url}/management/chart/${chartId}`, {
						headers: { Authorization: authHeader },
					});
				},
				upload: async function (
					title,
					composer,
					price,
					defaultId,
					introduction,
					startReview,
					expectedStatus,
					noterDisplayOverride,
					chart,
					musicFile,
					coverFile,
					previewFile,
					storeFile
				) {
					let form = new FormData();

					form.append("music-file", musicFile);
					form.append("cover-file", coverFile);
					form.append("preview-file", previewFile);
					if (storeFile != undefined) {
						form.append("store-preview-file", storeFile);
					}

					let chartMeta = chart.map((c, index) => {
						form.append(`chart-file-${index}`, c.file);
						return {
							chartFileName: `chart-file-${index}`,
							chartDifficultyClass: c.clazz,
							chartDifficultyValue: c.value,
							D: c.d,
							defaultId: c.defaultId,
						};
					});

					form.append(
						"chart-data",
						JSON.stringify({
							title: title,
							composerName: composer,
							chartMeta: chartMeta,
							musicFileName: "music-file",
							coverFileName: "cover-file",
							previewFileName: "preview-file",
							storePreviewFileName:
								storeFile == undefined ? null : "store-preview-file",
							coinPrice: price,
							defaultId: defaultId,
							introduction: introduction,
							startReview: startReview,
							expectedStatus: expectedStatus,
							noterDisplayOverride: noterDisplayOverride,
						})
					);

					return fetch(`${url}/management/upload`, {
						method: "post",
						credentials: "include",
						data: form,
					});
				},
			};
		},
	};
};
