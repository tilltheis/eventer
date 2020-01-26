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
    fetch(process.env.REACT_APP_API_URL + '/events')
      .then(res => res.json())
      .then(
        (result) => {
          this.setState({
            isLoaded: true,
            items: result
          });
        },
        (error) => {
          this.setState({
            isLoaded: true,
            error
          });
        }
      );
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
              <tr key={item.id}><td><a href={"/events/" + item.id}>✎</a></td><td>{item.title}</td><td>{item.host}</td><td>{item.dateTime}</td><td>Guests...</td><td>{item.description}</td></tr>
            ))}
          </tbody>
        </table>
      );
    }
  }
};