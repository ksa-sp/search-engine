# API Reference

## Start site indexing

#### Request

```http
  GET /api/startIndexing
```

| Parameter | Type     | Description                                                                                                                              |
|:----------|:---------|:-----------------------------------------------------------------------------------------------------------------------------------------|
| `site`    | `string` | Address of the site to start indexing. If not present - start indexing all sites configured. Format of the address `http://www.site.com` |

#### Response of success

      {
		"result": true
      }

#### Response of error

      {
		"result": false,
		"error": "Error description"
      }

## Stop site indexing

#### Request

```http
  GET /api/stopIndexing
```

| Parameter | Type     | Description                                                                                                                 |
|:----------|:---------|:----------------------------------------------------------------------------------------------------------------------------|
| `site`    | `string` | Address of the site to stop indexing. If not present - stop indexing all sites. Format of the address `http://www.site.com` |

#### Response of success

      {
		"result": true
      }

#### Response of error

      {
		"result": false,
		"error": "Error description"
      }

## One page indexing

#### Request

```http
  POST /api/indexPage
```

| Parameter | Type     | Description                                 |
|:----------|:---------|:--------------------------------------------|
| `url`     | `string` | **Required**. Address of the page to index. |

#### Response of success

      {
		"result": true
      }

#### Response of error

      {
		"result": false,
		"error": "Error description"
      }

## Request for statistics data

#### Request

```http
  GET /api/statistics
```

#### Response

      {
		"result": true,
		"statistics": {
			"total": {
				"sites": 10,
				"pages": 436423,
				"lemmas": 5127891,
				"indexing": true,
				"tasks": 10
			},
			"detailed": [
				{
					"url": "http://www.site.com",
					"name": "Site name",
					"status": "INDEXED",
					"statusTime": 1600160357,
					"error": "Error description or null if everything is ok",
					"pages": 5764,
					"lemmas": 321115,
					"tasks": 0
				},
				...
			]
		}
      }

## Search request

#### Request

```http
  GET /api/search
```

| Parameter | Type      | Description                                                                                                                      |
|:----------|:----------|:---------------------------------------------------------------------------------------------------------------------------------|
| `query`   | `string`  | **Required**. Search query.                                                                                                      |
| `site`    | `string`  | Address of the site to search in. If not present - search through all sites indexed. Format of the address `http://www.site.com` |
| `offset`  | `integer` | First search result shift relative to total results list. Default value is 0.                                                    |
| `limit`   | `integer` | Maximum number of search results in response. Default value is 20.                                                               |

#### Response of success

      {
		"result": true,
		"count": 20,
		"data": [
			{
				"site": "http://www.site.com",
				"siteName": "Site name",
				"uri": "/path/to/page/6784",
				"title": "Page title",
				"snippet": "Part of the page text with <b>query words bolded</b>",
				"relevance": 0.93362
			},
			...
		]
      }

#### Response of error

      {
		"result": false,
		"error": "Error description"
      }