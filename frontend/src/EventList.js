import React from 'react';

export default class EventList extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      error: null,
      isLoaded: false,
      items: []
    };
  }

  componentDidMount() {
    fetch("http://localhost:8080/events")
      .then(res => res.json())
      .then(
        (result) => {
          this.setState({
            isLoaded: true,
            items: result
          });
        },
        // Note: it's important to handle errors here
        // instead of a catch() block so that we don't swallow
        // exceptions from actual bugs in components.
        (error) => {
          this.setState({
            isLoaded: true,
            error
          });
        }
      )
  }

  render() {
    const { error, isLoaded, items } = this.state;
    if (error) {
      return <div>Error: {error.message}</div>;
    } else if (!isLoaded) {
      return <div>Loading...</div>;
    } else if (items.length === 0) {
      return <div>There are no events.</div>;
    } else {
      return (
        <table>
          <thead>
            <tr><td></td><th>Title</th><th>Host</th><th>Date</th><th>Guests</th><th>Description</th></tr>
          </thead>
          <tbody>
            {items.map(item => (
              <tr key={item.id}><td><a href={"/events/" + item.id}>✎</a></td><td>{item.title}</td><td>{item.host}</td><td>{item.createdAt}</td><td>Guests...</td><td>{item.description}</td></tr>
            ))}
          </tbody>
        </table>
      );
    }
  }
};